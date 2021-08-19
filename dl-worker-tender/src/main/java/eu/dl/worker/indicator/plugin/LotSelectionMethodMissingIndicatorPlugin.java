package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.codetables.SelectionMethod;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;
import org.apache.commons.lang3.ObjectUtils;


/**
 * Missing notice selection method.
 */
public final class LotSelectionMethodMissingIndicatorPlugin extends BaseLotKeyMissingIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_SELECTION_METHOD.name();

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
        IndicatorScore score = new IndicatorScore();

        SelectionMethod method = ObjectUtils.firstNonNull(lot.getSelectionMethod(), tender.getSelectionMethod());
        score.test(method != null);
        return calculated(score.ratio());
    }
}
