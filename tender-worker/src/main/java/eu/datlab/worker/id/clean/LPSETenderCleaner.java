package eu.datlab.worker.id.clean;

import eu.datlab.worker.clean.BaseDatlabTenderCleaner;
import eu.dl.dataaccess.dto.clean.CleanTender;
import eu.dl.dataaccess.dto.parsed.ParsedTender;
import eu.dl.worker.clean.plugin.AwardCriteriaPlugin;
import eu.dl.worker.clean.plugin.BodyPlugin;
import eu.dl.worker.clean.plugin.DatePlugin;
import eu.dl.worker.clean.plugin.DateTimePlugin;
import eu.dl.worker.clean.plugin.LotPlugin;
import eu.dl.worker.clean.plugin.PricePlugin;
import eu.dl.worker.clean.plugin.PublicationPlugin;
import eu.dl.worker.clean.plugin.TenderProcedureTypePlugin;
import eu.dl.worker.clean.plugin.TenderSupplyTypePlugin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tender cleaner for Indonesia.
 *
 * @author Marek Mikes
 */
public class LPSETenderCleaner extends BaseDatlabTenderCleaner {
    private static final String VERSION = "1.0";

    private static final Locale LOCALE = new Locale("id");

    private static final NumberFormat NUMBER_FORMAT;
    static {
        DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(LOCALE);
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(',');
        NUMBER_FORMAT = new DecimalFormat("#,##0.###", formatSymbols);
    }

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("d MMMM uuuu[ H:m]", LOCALE);

    @Override
    public final String getVersion() {
        return VERSION;
    }

    @Override
    protected final ParsedTender preProcessParsedItem(final ParsedTender parsedItem) {
        return parsedItem;
    }

    @Override
    protected final CleanTender postProcessSourceSpecificRules(final ParsedTender parsedTender, final CleanTender cleanTender) {
        return cleanTender;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected final void registerSpecificPlugins() {
        pluginRegistry
            .registerPlugin("date", new DatePlugin(DATETIME_FORMATTER))
            .registerPlugin("datetime", new DateTimePlugin(DATETIME_FORMATTER))
            .registerPlugin("bodies", new BodyPlugin(null, null, null))
            .registerPlugin("lots", new LotPlugin(NUMBER_FORMAT, DATETIME_FORMATTER, Collections.emptyMap()))
            .registerPlugin("publications", new PublicationPlugin(NUMBER_FORMAT, DATETIME_FORMATTER, getFormTypeMapping()))
            .registerPlugin("procedureType", new TenderProcedureTypePlugin(getProcedureTypeMapping(), null))
            .registerPlugin("supplyType", new TenderSupplyTypePlugin(getSupplyTypeMapping()))
            .registerPlugin("prices", new PricePlugin(NUMBER_FORMAT))
            .registerPlugin("awardCriterion", new AwardCriteriaPlugin(NUMBER_FORMAT));
    }

    /**
     * @return form type mapping
     */
    private Map<Enum, List<String>> getFormTypeMapping() {
        final Map<Enum, List<String>> mapping = new HashMap<>();
        return mapping;
    }

    /**
     * @return form type mapping
     */
    private Map<Enum, List<String>> getSupplyTypeMapping() {
        final Map<Enum, List<String>> mapping = new HashMap<>();
        return mapping;
    }

    /**
     * @return procedure type mapping for cleaning process
     */
    private static Map<Enum, List<String>> getProcedureTypeMapping() {
        final Map<Enum, List<String>> mapping = new HashMap<>();
        return mapping;
    }
}
