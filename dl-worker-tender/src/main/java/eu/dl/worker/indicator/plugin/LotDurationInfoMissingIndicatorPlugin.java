package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Missing or incomplete contract notice duration info.
 */
public final class LotDurationInfoMissingIndicatorPlugin extends BaseLotKeyMissingIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_OR_INCOMPLETE_DURATION_INFO.name();

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    protected PublicationFormType getFormType() {
        return PublicationFormType.CONTRACT_NOTICE;
    }

    @Override
    protected Indicator getIndicator(final MasterTenderLot lot, final MasterTender tender) {
        boolean hasDuration = ObjectUtils.anyNotNull(
            lot.getEstimatedDurationInDays(),
            lot.getEstimatedDurationInMonths(),
            lot.getEstimatedDurationInYears()
        );

        boolean hasStart = lot.getEstimatedStartDate() != null;
        boolean hasEnd = lot.getEstimatedCompletionDate() != null;

        IndicatorScore score = new IndicatorScore();
        score.test(hasStart || hasEnd);
        score.test(hasDuration);

        return calculated(score.ratio());
    }
}
