package eu.dl.worker.indicator.plugin;

import eu.dl.core.config.Config;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;

/**
 * This plugin calculates corruption decision period indicator.
 */
public class LotDecisionPeriodIndicatorPlugin extends BaseLotIndicatorPlugin {
    @Override
    public final Indicator evaluate(final MasterTenderLot lot, final MasterTender tender) {
        if (tender == null || tender.getCountry() == null) {
            return insufficient();
        }

        if(Config.getInstance().getParam("indicator." + tender.getCountry() + ".decisionPeriod.100.length") == null) {
            return undefined();
        }

        if (tender.getBidDeadline() == null) {
            double missingFlagValue = isMissingBidDeadlineRedFlag(tender.getCountry());
            return calculated(missingFlagValue);
        } else if (lot.getAwardDecisionDate() != null) {
            long periodLength = ChronoUnit.DAYS.between(tender.getBidDeadline().toLocalDate(), lot.getAwardDecisionDate());
            HashMap<String, Object> metaData = new HashMap<>();
            metaData.put("decisionPeriodLength", periodLength);

            if (periodLength < 0) {
                return insufficient();
            } else {
                double redFlagValue = checkDecisionPeriod(tender.getCountry(), periodLength);
                return calculated(redFlagValue, metaData);
            }
        } else {
            return IndicatorUtils.hasAward(tender) ? insufficient() : undefined();
        }
    }

    /**
     * Checks whether missing bidDeadline means red flag for a given country.
     * @param countryCode country code
     * @return 100 if missing bid deadline is considered as non problematic, 50 if level 1, 0 if level 2
     */
    private double isMissingBidDeadlineRedFlag(final String countryCode) {
        // get configuration for given country
        String value = Config.getInstance().getParam("indicator." + countryCode + ".decisionPeriod.missing");
        if(value == null || value.equals("100")) {
            return 100d;
        } else if (value.equals("50")) {
            return 50d;
        } else {
            return 0d;
        }
    }

    /**
     * Check whether the decision period is of problematic length.
     *
     * @param countryCode country code
     * @param decisionPeriodLength decision period length
     *
     * @return 100 if not problematic, 50 if level 1, 0 if level 2
     */
    private double checkDecisionPeriod(final String countryCode, final Long decisionPeriodLength) {
        if(IndicatorUtils.isValueInPeriod(Config.getInstance().getParam(
                "indicator." + countryCode + ".decisionPeriod.100.length"), decisionPeriodLength)) {
            return 100d;
        } else if (IndicatorUtils.isValueInPeriod(Config.getInstance().getParam(
                "indicator." + countryCode + ".decisionPeriod.50.length"), decisionPeriodLength)) {
            return 50d;
        } else {
            return 0d;
        }
    }

    @Override
    public final String getType() {
        return TenderIndicatorType.INTEGRITY_DECISION_PERIOD.name();
    }
}
