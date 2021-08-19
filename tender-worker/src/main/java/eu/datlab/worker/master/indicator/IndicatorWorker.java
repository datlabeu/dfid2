package eu.datlab.worker.master.indicator;

import eu.datlab.dataaccess.dao.DAOFactory;
import eu.dl.dataaccess.dao.MatchedTenderDAO;
import eu.dl.dataaccess.dto.master.MasterTenderLot;
import eu.dl.dataaccess.utils.PopulateUtils;
import eu.dl.dataaccess.dao.MasterBodyDAO;
import eu.dl.dataaccess.dao.MasterTenderDAO;
import eu.dl.dataaccess.dao.TransactionUtils;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.worker.BaseWorker;
import eu.dl.worker.Message;
import eu.dl.worker.indicator.plugin.IndicatorPlugin;
import eu.dl.worker.indicator.plugin.LotIndicatorPlugin;
import eu.dl.worker.utils.BasicPluginRegistry;
import eu.dl.worker.utils.PluginRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This worker calculates indicators for tenders.
 *
 * @author Jakub Krafka
 */
public class IndicatorWorker extends BaseWorker {

    private static final String INCOMING_EXCHANGE_NAME = "master";

    private static final String OUTGOING_EXCHANGE_NAME = "master";

    private static final String VERSION = "1.0";

    private static TransactionUtils transactionUtils;

    private static MasterTenderDAO masterDao;

    private static MasterBodyDAO masterBodyDao;

    private static PopulateUtils populateUtils;

    protected PluginRegistry<IndicatorPlugin<MasterTender>> tenderIndicatorPluginRegistry = new BasicPluginRegistry();

    protected PluginRegistry<LotIndicatorPlugin> lotIndicatorPluginRegistry = new BasicPluginRegistry();

    /**
     * Template for tender level plugin class name. Plugin name needs to be inserted for particular plugin.
     */
    private static final String TENDER_LEVEL_PLUGIN_CLASS_NAME_TEMPLATE = "eu.dl.worker.indicator.plugin.%sIndicatorPlugin";

    /**
     * Template for lot level plugin class name. Plugin name needs to be inserted for particular plugin.
     */
    private static final String LOT_LEVEL_PLUGIN_CLASS_NAME_TEMPLATE = "eu.dl.worker.indicator.plugin.Lot%sIndicatorPlugin";

    /**
     * Class name of WinnerCAShare lot level plugin. It's located in tender-worker because it needs to access masterDAO.
     */
    private static final String LOT_WINNER_CA_SHARE_PLUGIN_CLASS_NAME = "eu.datlab.worker.master.plugin.LotWinnerCAShareIndicatorPlugin";


    /**
     * Initialization of everything.
     */
    public IndicatorWorker() {
        super();
        transactionUtils = DAOFactory.getDAOFactory().getTransactionUtils();

        masterDao = DAOFactory.getDAOFactory().getMasterTenderDAO(getName(), VERSION);

        masterBodyDao = DAOFactory.getDAOFactory().getMasterBodyDAO(getName(), VERSION);

        populateUtils = new PopulateUtils(masterBodyDao);

        config.addConfigFile("indicator");

        registerIndicatorPlugins();
    }

    @Override
    protected final String getVersion() {
        return VERSION;
    }

    @Override
    protected final String getIncomingExchangeName() {
        return INCOMING_EXCHANGE_NAME;
    }

    @Override
    protected final String getIncomingQueueName() {
        return getIncomingQueueNameFromConfig();
    }

    @Override
    protected final String getOutgoingExchangeName() {
        return OUTGOING_EXCHANGE_NAME;
    }

    @Override
    public final void doWork(final Message message) {
        String id = message.getValue("id");
        final MasterTender tender = masterDao.getById(id);

        if (tender != null) {
            populateUtils.populateBodies(Arrays.asList(tender));

            // iterate over all item indicator plugins and execute them in a proper order
            List<Indicator> tenderIndicators = new ArrayList<>();
            for (Entry<String, IndicatorPlugin<MasterTender>> entry : tenderIndicatorPluginRegistry.getPlugins().entrySet()) {
                IndicatorPlugin<MasterTender> plugin = entry.getValue();
                Indicator indicator = plugin.evaluate(tender);
                if (indicator != null) {
                    tenderIndicators.add(indicator);
                }
            }
            tender.setIndicators(tenderIndicators);

            if (tender.getLots() != null) {
                for (MasterTenderLot lot : tender.getLots()) {
                    List<Indicator> lotIndicators = new ArrayList<>();
                    // iterate over all item indicator plugins and execute them in a proper order
                    for (Entry<String, LotIndicatorPlugin> entry : lotIndicatorPluginRegistry.getPlugins().entrySet()) {
                        LotIndicatorPlugin plugin = entry.getValue();
                        Indicator indicator = plugin.evaluate(lot, tender);
                        if (indicator != null) {
                            lotIndicators.add(indicator);
                        }
                    }
                    lot.setIndicators(lotIndicators);
                }
            }

            populateUtils.depopulateBodies(Arrays.asList(tender));
            masterDao.save(tender);
        }
    }

    @Override
    protected final void resend(final String version, final String dateFrom, final String dateTo) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected final TransactionUtils getTransactionUtils() {
        return DAOFactory.getDAOFactory().getTransactionUtils();
    }

    /**
     * Registers indicator plugins.
     */
    protected final void registerIndicatorPlugins() {
        String indicatorsConfiguration = config.getParam("indicators.config");
        if(indicatorsConfiguration == null) {
            logger.error("No indicators configuration found! No plugins will be registered");
            return;
        }
        IndicatorPlugin pluginInstance = null;

        // tender level plugins
        Set<String> tenderLevelPluginNames = config.getParamValueAsList(indicatorsConfiguration + ".tenderLevel.plugins", ",",
                HashSet.class);
        if(tenderLevelPluginNames == null || tenderLevelPluginNames.isEmpty()) {
            logger.warn("No tender level plugins found in configuration for key {}", indicatorsConfiguration + ".tenderLevel.plugins");
        } else {
            for(Object pluginName: tenderLevelPluginNames) {
                // IndicatorPlugin<MasterTender>
                pluginInstance = getPluginInstanceByName(String.format(TENDER_LEVEL_PLUGIN_CLASS_NAME_TEMPLATE, (String) pluginName));
                if(pluginInstance == null) {
                    continue;
                }
                tenderIndicatorPluginRegistry.registerPlugin(pluginInstance.getType(), pluginInstance);
            }
        }

        // lot level plugins
        Set<String> lotLevelPluginNames = config.getParamValueAsList(indicatorsConfiguration + ".lotLevel.plugins", ",",
                HashSet.class);
        if(lotLevelPluginNames == null || lotLevelPluginNames.isEmpty()) {
            logger.warn("No lot level plugins found in configuration for key {}", indicatorsConfiguration + ".lotLevel.plugins");
        } else {
            for(Object pluginName: lotLevelPluginNames) {
                // WinnerCAShare is the only plugin, located in tender-worker, so it has specific class name
                if(pluginName.equals("WinnerCAShare")) {
                    pluginInstance = getPluginInstanceByName(LOT_WINNER_CA_SHARE_PLUGIN_CLASS_NAME);
                } else {
                    pluginInstance = getPluginInstanceByName(String.format(LOT_LEVEL_PLUGIN_CLASS_NAME_TEMPLATE, (String) pluginName));
                }
                if(pluginInstance == null) {
                    continue;
                }
                lotIndicatorPluginRegistry.registerPlugin(pluginInstance.getType(), (LotIndicatorPlugin) pluginInstance);
            }
        }
    }

    /**
     * Instantiate plugin by the class name.
     * @param pluginName class name of plugin
     * @return plugin instance
     */
    private IndicatorPlugin getPluginInstanceByName(final String pluginName) {
        Class<?> pluginClass = null;
        try {
            pluginClass = Class.forName(pluginName);
            Object pluginInstance = null;
            // the only plugin with parameters is NoticeAndAwardDiscrepancies
            if(pluginName.contains("NoticeAndAwardDiscrepancies")) {
                Constructor<?> constructor = pluginClass.getConstructor(MatchedTenderDAO.class);
                pluginInstance = constructor.newInstance(
                        DAOFactory.getDAOFactory().getMatchedTenderDAO(null, null, Collections.emptyList()));
            } else {
                pluginInstance = pluginClass.getConstructor().newInstance();
            }
            return (IndicatorPlugin) pluginInstance;
        } catch (ClassNotFoundException e) {
            logger.info("No plugin with name {} found", pluginName);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            logger.info("No such method in class {}: {}", pluginName, e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("Unable to invoke method for class {}: {}", pluginName, e.getMessage());
            e.printStackTrace();
        } catch (InstantiationException e) {
            logger.error("Unable to instantiate class {}: {}", pluginName, e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
