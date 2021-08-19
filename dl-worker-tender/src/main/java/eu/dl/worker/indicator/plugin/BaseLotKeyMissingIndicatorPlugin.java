package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;
import eu.dl.dataaccess.utils.TenderUtils;

/**
 * Basic plugin for missing lot field.
 */
abstract class BaseLotKeyMissingIndicatorPlugin extends BaseIndicatorPlugin implements LotIndicatorPlugin {

    @Override
    public Indicator evaluate(final MasterTenderLot lot, final MasterTender tender) {
        if (lot == null || tender == null) {
            return insufficient();
        }

        if (TenderUtils.hasPublicationOfType(getFormType()).negate().test(tender)) {
            return undefined();
        }

        return getIndicator(lot, tender);
    }

    @Override
    public final Indicator evaluate(final MasterTenderLot lot) {
        return evaluate(lot, null);
    }

    /**
     * @return publication form type
     */
    protected abstract PublicationFormType getFormType();

    /**
     * Evaluates indicator for master lot.
     *
     * @param lot
     *      master tender lot
     * @param tender
     *      master tender (lot's context)
     * @return evaluated indicator. Shouldn't returns null, never.
     */
    protected abstract Indicator getIndicator(MasterTenderLot lot, MasterTender tender);
}
