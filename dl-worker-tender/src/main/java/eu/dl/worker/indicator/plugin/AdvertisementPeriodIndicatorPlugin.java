package eu.dl.worker.indicator.plugin;

import eu.dl.core.config.Config;
import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.generic.Publication;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;
import eu.dl.dataaccess.dto.master.MasterTender;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

/**
 * This plugin calculates advertisement period length.
 */
public class AdvertisementPeriodIndicatorPlugin extends BaseIndicatorPlugin implements IndicatorPlugin<MasterTender> {

    @Override
    public final Indicator evaluate(final MasterTender tender) {
        if (tender == null || tender.getCountry() == null) {
            return insufficient();
        }

        if(Config.getInstance().getParam("indicator." + tender.getCountry() + ".advertisementPeriod.100.length") == null) {
            return undefined();
        }

        if (tender.getBidDeadline() == null) {
            double missingFlagValue = isMissingBidDeadlineRedFlag(tender.getCountry());
            return calculated(missingFlagValue);
        } else {
            if (tender.getPublications() == null) {
                return insufficient();
            }

            // iterate over lots and pick the oldest callForTenderDate
            LocalDate oldestCallForTenderDate = null;
            for (Publication publication: tender.getPublications()) {
                if (publication.getFormType() != null
                        && publication.getFormType().equals(PublicationFormType.CONTRACT_NOTICE)
                        && publication.getPublicationDate() != null
                        && (oldestCallForTenderDate == null
                            || oldestCallForTenderDate.isAfter(publication.getPublicationDate()))) {
                        oldestCallForTenderDate = publication.getPublicationDate();
                }
            }

            if (oldestCallForTenderDate == null) {
                return insufficient();
            }

            Long advertisementPeriodLength = ChronoUnit.DAYS.between(
                    oldestCallForTenderDate, LocalDate.from(tender.getBidDeadline()));
            HashMap<String, Object> metaData = new HashMap<String, Object>();
            metaData.put("advertisementPeriodLength", advertisementPeriodLength);
            metaData.put("callForTenderDate", oldestCallForTenderDate);
            metaData.put("bidDeadline", tender.getBidDeadline());
            if (advertisementPeriodLength.compareTo(0L) < 0) {
                // the period is negative
                return insufficient(metaData);
            } else {
                double redFlagValue = checkAdvertisementPeriod(tender.getCountry(), advertisementPeriodLength);
                return calculated(redFlagValue, metaData);
            }
        }
    }

    @Override
    public final String getType() {
        return TenderIndicatorType.INTEGRITY_ADVERTISEMENT_PERIOD.name();
    }

    /**
     * Checks whether missing bidDeadline means red flag for a given country.
     * @param countryCode country code
     * @return 100 if missing bid deadline is considered as non problematic, 50 if level 1, 0 if level 2
     */
    private double isMissingBidDeadlineRedFlag(final String countryCode) {
        // get configuration for given country
        String value = Config.getInstance().getParam("indicator." + countryCode + ".advertisementPeriod.missing");
        if(value == null || value.equals("100")) {
            return 100d;
        } else if (value.equals("50")) {
            return 50d;
        } else {
            return 0d;
        }
    }

    /**
     * Check whether the advertisement period is of problematic length.
     *
     * @param countryCode country code
     * @param advertisementPeriodLength advertisement period length
     *
     * @return 100 if not problematic, 50 if level 1, 0 if level 2
     */
    private double checkAdvertisementPeriod(final String countryCode, final Long advertisementPeriodLength) {
        if(IndicatorUtils.isValueInPeriod(Config.getInstance().getParam(
                "indicator." + countryCode + ".advertisementPeriod.100.length"), advertisementPeriodLength)) {
            return 100d;
        } else if (IndicatorUtils.isValueInPeriod(Config.getInstance().getParam(
                "indicator." + countryCode + ".advertisementPeriod.50.length"), advertisementPeriodLength)) {
            return 50d;
        } else {
            return 0d;
        }
    }
}
