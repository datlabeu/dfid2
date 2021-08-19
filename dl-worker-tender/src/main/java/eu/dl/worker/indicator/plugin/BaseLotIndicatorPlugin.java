package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.master.MasterTenderLot;

/**
 * Basic lot indicator plugin.
 */
public abstract class BaseLotIndicatorPlugin extends BaseIndicatorPlugin implements LotIndicatorPlugin {
    @Override
    public final Indicator evaluate(final MasterTenderLot lot) {
        return evaluate(lot, null);
    }
}
