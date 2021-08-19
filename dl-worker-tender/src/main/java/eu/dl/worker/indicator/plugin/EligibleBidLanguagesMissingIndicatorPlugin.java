package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;
import eu.dl.dataaccess.dto.master.MasterTender;


/**
 * Missing notice eligible bid languages.
 */
public final class EligibleBidLanguagesMissingIndicatorPlugin extends BaseKeyMissingIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_ELIGIBLE_BID_LANGUAGES.name();

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    protected PublicationFormType getFormType() {
        return PublicationFormType.CONTRACT_NOTICE;
    }

    @Override
    protected Indicator getIndicator(final MasterTender tender) {
        IndicatorScore score = new IndicatorScore();
        score.test(tender.getEligibleBidLanguages() != null);
        return calculated(score.ratio());
    }
}
