package eu.datlab.worker.master.statistic;

import eu.datlab.dataaccess.dao.DAOFactory;
import eu.dl.core.UnrecoverableException;
import eu.dl.dataaccess.dao.MasterBodyDAO;
import eu.dl.dataaccess.dao.MasterTenderDAO;
import eu.dl.dataaccess.dao.TransactionUtils;
import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.codetables.TenderSupplyType;
import eu.dl.dataaccess.dto.generic.CPV;
import eu.dl.dataaccess.dto.generic.Publication;
import eu.dl.dataaccess.dto.master.MasterBody;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;
import eu.dl.dataaccess.utils.DigestUtils;
import eu.dl.worker.BaseWorker;
import eu.dl.worker.Message;
import org.apache.lucene.search.spell.NGramDistance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * This worker calculates a value which indicates whether we shall pass particular tender to opentender.eu portal or
 * not.
 *
 * @author Tomas Mrazek
 */
public final class OpenTenderWorker extends BaseWorker {

    private static final String INCOMING_EXCHANGE_NAME = "master";

    private static final String OUTGOING_EXCHANGE_NAME = "master";

    private static final String VERSION = "1.0";

    private static TransactionUtils transactionUtils;

    private static MasterTenderDAO masterTenderDAO;
    private static MasterBodyDAO esNewMasterBodyDAO;
    private static MasterBodyDAO esOldMasterBodyDAO;

    protected static final double SIMILARITY_THRESHOLD = 4;

    protected static final List<String> COUNTRIES = Arrays.asList("DE", "IT", "SE", "BE", "FI", "AT", "DK", "GR", "LU",
        "CY", "MT", "IS", "RS", "AM");

    protected static final BigDecimal THRESHOLD = new BigDecimal(135000);
    protected static final BigDecimal THRESHOLD_WORKS = new BigDecimal(5186000);

    protected static final String WORKER_PL = "eu.datlab.worker.pl.master.UZPTenderMaster";
    protected static final String WORKER_GE = "eu.datlab.worker.ge.master.SPATenderMaster";
    protected static final String WORKER_OLD_ES = "eu.datlab.worker.es.master.PCETenderMaster";
    protected static final String WORKER_NEW_ES = "eu.datlab.worker.es.master.HaciendaTenderMaster";
    protected static final String WORKER_OLD_RO = "eu.datlab.worker.ro.master.APATenderMaster";
    protected static final String WORKER_NEW_RO = "eu.datlab.worker.ro.master.SICAPTenderMaster";
    protected static final List<String> WORKERS_EU = Arrays.asList("eu.datlab.worker.eu.master.TedTenderMaster",
        "eu.datlab.worker.eu.master.TedCSVTenderMaster");

    /**
     * Default constructor with initialization of everything.
     */
    public OpenTenderWorker() {
        super();
        transactionUtils = DAOFactory.getDAOFactory().getTransactionUtils();
        masterTenderDAO = DAOFactory.getDAOFactory().getMasterTenderDAO(getName(), VERSION);
        esNewMasterBodyDAO = DAOFactory.getDAOFactory().getMasterBodyDAO(WORKER_NEW_ES, null);
        esOldMasterBodyDAO = DAOFactory.getDAOFactory().getMasterBodyDAO(WORKER_OLD_ES, null);
    }

    @Override
    protected String getVersion() {
        return VERSION;
    }

    @Override
    protected String getIncomingExchangeName() {
        return INCOMING_EXCHANGE_NAME;
    }

    @Override
    protected String getIncomingQueueName() {
        return getIncomingQueueNameFromConfig();
    }

    @Override
    protected String getOutgoingExchangeName() {
        return OUTGOING_EXCHANGE_NAME;
    }

    @Override
    public void doWork(final Message message) {
        String id = message.getValue("id");

        final MasterTender tender = masterTenderDAO.getById(id);

        if (tender != null) {
            HashMap<String, Object> metaData = Optional.ofNullable(tender.getMetaData()).orElse(new HashMap<>());

            // do not overwrite already calculated
            if (metaData.containsKey("opentender")) {
                return;
            }

            metaData.put("opentender", isOpenTender(tender));
            tender.setMetaData(metaData);
            masterTenderDAO.save(tender);
        }
    }

    @Override
    protected void resend(final String version, final String dateFrom, final String dateTo) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected TransactionUtils getTransactionUtils() {
        return DAOFactory.getDAOFactory().getTransactionUtils();
    }

    /**
     * Checks whether the given tender is suitable for opentender export.
     *
     * @param tender
     *      master tender to parse from
     * @return true only and only if the tender is suitable for open tender export, otherwise false.
     */
    protected static boolean isOpenTender(final MasterTender tender) {
        if (tender == null || tender.getCreatedBy() == null) {
            return false;
        }

        if ((WORKERS_EU.contains(tender.getCreatedBy()) && COUNTRIES.contains(tender.getCountry()))
            || tender.getCreatedBy().equals(WORKER_PL)
            || tender.getCreatedBy().equals(WORKER_GE)) {

            return true;
        } else {
            BigDecimal price = getPrice(tender);

            List<String> workers = new ArrayList<>(WORKERS_EU);
            workers.add(WORKER_GE);
            workers.add(WORKER_PL);

            BigDecimal threshold = isWorks(tender) ? THRESHOLD_WORKS : THRESHOLD;

            if (price == null
                || (WORKERS_EU.contains(tender.getCreatedBy()) && !COUNTRIES.contains(tender.getCountry())
                    && price.compareTo(threshold) > 0)
                    || (!workers.contains(tender.getCreatedBy()) && price.compareTo(threshold) <= 0)) {
                if ((tender.getCreatedBy().equals(WORKER_OLD_ES) && isOnSourceES(tender))
                        || (tender.getCreatedBy().equals(WORKER_OLD_RO) && isOnSourceRO(tender))) {
                    // If tender is duplicated on both old and new sources sources, use only one from the new
                    // source (for ES, RO workers only)
                    return false;
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if given tender is on new ES source based on buyerAssignedId, title, buyerName and worker.
     *
     * @param tender      tender to check
     * @return true if source contains tender with the same buyerAssignedId and similar title and buyer name as given
     * tender and created by given worker, false otherwise.
     */
    private static boolean isOnSourceES(final MasterTender tender) {
        if(tender == null || tender.getBuyerAssignedId() == null || tender.getBuyers() == null || tender.getBuyers().isEmpty()
        || tender.getTitle() == null) {
            return false;
        }

        String buyerAssignedId = tender.getBuyerAssignedId();

        List<MasterTender> tenders = masterTenderDAO.getByBuyerAssignedId(buyerAssignedId);
        tenders.removeIf(t -> !t.getCreatedBy().equals(WORKER_NEW_ES));

        if(tenders.isEmpty()) {
            return false;
        }
        String title = DigestUtils.removeAccents(tender.getTitle()).toLowerCase();
        String buyerName = DigestUtils.removeAccents(getFirstBuyerName(tender, false)).toLowerCase();

        return tenders.stream().filter(Objects::nonNull)
                .anyMatch(t ->
                        (new NGramDistance(3).getDistance(title,
                                DigestUtils.removeAccents(t.getTitle()).toLowerCase()) > 0.6)
                                &&
                                (new NGramDistance(3).getDistance(buyerName,
                                        DigestUtils.removeAccents(getFirstBuyerName(t, true)).toLowerCase()) > 0.6));
    }

    /**
     * Gets name of first buyer for which name is not missing.
     *
     * @param tender    tender
     * @param newWorker indicates if worker is new or old
     * @return name or empty string if no buyer name is available
     */
    private static String getFirstBuyerName(final MasterTender tender, final boolean newWorker) {
        if (tender.getBuyers() == null || tender.getBuyers().stream().filter(Objects::nonNull).noneMatch(b -> b.getGroupId() != null)) {
            return "";
        }
        String buyerGroupId = tender.getBuyers().stream().filter(Objects::nonNull)
                .filter(b -> b.getGroupId() != null).findFirst().orElse(new MasterBody()).getGroupId();
        List<MasterBody> buyerList =
                (List<MasterBody>) (newWorker ? esNewMasterBodyDAO : esOldMasterBodyDAO).getByGroupId(buyerGroupId);
        if(buyerList.size() > 1) {
            throw new UnrecoverableException("There are more mastered instances with the same groupId.");
        } else if (buyerList.isEmpty()) {
            return "";
        }
        MasterBody buyer = buyerList.get(0);
        if (buyer == null || buyer.getName() == null) {
            return "";
        }
        return buyer.getName();
    }

    /**
     * Checks if given tender is on new RO source based on sourceId and worker.
     *
     * @param tender      tender to check
     * @return true if source contains tender with the same sourceId as buyerAssignedId of given tender and created by given worker, false
     * otherwise.
     */
    private static boolean isOnSourceRO(final MasterTender tender) {
        // deduplication of RO sources is provided based on buyerAssignedId from CONTRACT_AWARD publication from APA source and
        // sourceId from publication from SICAP source.
        if(tender == null || tender.getPublications() == null || tender.getPublications().stream()
                .noneMatch(p -> p.getFormType() != null && p.getFormType().equals(PublicationFormType.CONTRACT_AWARD))) {
            return false;
        }

        String buyerAssignedId = tender.getPublications().stream().filter(Objects::nonNull).map(Publication::getBuyerAssignedId)
                    .filter(Objects::nonNull).findFirst().orElse(null);

        // Search for tenders witch sourceId is equal to buyerAssignedId of given tender.
        List<MasterTender> tenders = masterTenderDAO.getBySourceId(buyerAssignedId);
        tenders.removeIf(t -> !t.getCreatedBy().equals(WORKER_NEW_RO));
        return !tenders.isEmpty();
    }

    /**
     * @param tender
     *      master tender
     * @return TRUE if the given tender is WORKS
     */
    private static boolean isWorks(final MasterTender tender) {
        if (tender == null) {
            return false;
        }

        if (tender.getSupplyType() != null) {
            return  TenderSupplyType.WORKS == tender.getSupplyType();
        } else {
            boolean tenderIsWorks = tender.getCpvs() != null && tender.getCpvs().stream()
                .filter(n -> Boolean.TRUE.equals(n.getIsMain()))
                .findFirst().map(OpenTenderWorker::isWorksCPV).orElse(false);

            boolean lotIsWorks = tender.getLots() != null && tender.getLots().stream()
                .map(MasterTenderLot::getCpvs).filter(Objects::nonNull).flatMap(n -> n.stream())
                .filter(n -> Boolean.TRUE.equals(n.getIsMain())).findFirst().map(OpenTenderWorker::isWorksCPV).orElse(false);

            return tenderIsWorks || lotIsWorks;
        }
    }

    /**
     * @param cpv
     *      cpv
     * @return TRUE in case that CVP code starts with 45, otherwise FALSE
     */
    private static boolean isWorksCPV(final CPV cpv) {
        return cpv == null || cpv.getCode() == null ? false : cpv.getCode().startsWith("45");
    }

    /**
     * Returns tender price acording to following rules.
     *
     * <ul>
     *      <li>if is not null return tender.finalPrice.netAmountEur</li>
     *      <li>else if is not null return tender.estimatedPrice.netAmountEur</li>
     *      <li>else if not null return sum(lot[i].bid[isWinning=true].price.netAmountEur)</li>
     *      <li>else if not null return sum(lot[i].estimatedPrice.netAmountEur)</li>
     *      <li>else return null</li>
     * </ul>
     *
     * @param tender
     *      master tender to parse from
     * @return price of the tender or null
     */
    protected static BigDecimal getPrice(final MasterTender tender) {
        if (tender == null) {
            return null;
        }

        if (tender.getFinalPrice() != null && tender.getFinalPrice().getNetAmountEur() != null) {
            return tender.getFinalPrice().getNetAmountEur();
        } else if (tender.getEstimatedPrice()!= null && tender.getEstimatedPrice().getNetAmountEur() != null) {
            return tender.getEstimatedPrice().getNetAmountEur();
        }


        if (tender.getLots() != null) {
            // sum of the net amount prices in euros of winning bids of all lots.
            Supplier<Stream<BigDecimal>> sum = () -> tender.getLots().stream()
                .flatMap(n -> {
                    if (n.getBids() == null) {
                        return Stream.empty();
                    }
                    
                    // get all non-null net amount prices in euros of winning bids.
                    return n.getBids().stream()
                        .filter(m -> Objects.equals(m.getIsWinning(), Boolean.TRUE) && m.getPrice() != null
                            &&  m.getPrice().getNetAmountEur() != null)
                        .map(m -> m.getPrice().getNetAmountEur());
                });

            // sum of the net amount estimated prices in euros of all lots.
            if (sum.get().count() == 0) {
                sum = () -> tender.getLots().stream()
                    .filter(n -> n.getEstimatedPrice() != null && n.getEstimatedPrice().getNetAmountEur() != null)
                    .map(n -> n.getEstimatedPrice().getNetAmountEur());
            }
            

            return sum.get().count() == 0 ? null : sum.get().reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return null;
    }
}
