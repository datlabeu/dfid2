package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.utils.TenderUtils;

/**
 * Basic plugin for missing tender field.
 */
abstract class BaseKeyMissingIndicatorPlugin extends BaseIndicatorPlugin implements IndicatorPlugin<MasterTender> {

    @Override
    public final Indicator evaluate(final MasterTender tender) {
        if (tender == null) {
            return insufficient();
        }

        if (TenderUtils.hasPublicationOfType(getFormType()).negate().test(tender)) {
            return undefined();
        }

        return getIndicator(tender);
    }

    /**
     * @return publication form type
     */
    protected abstract PublicationFormType getFormType();

    /**
     * Evaluates indicator for master tender.
     *
     * @param tender
     *      master tender
     * @return evaluated indicator. Shouldn't returns null, never.
     */
    protected abstract Indicator getIndicator(MasterTender tender);
}
