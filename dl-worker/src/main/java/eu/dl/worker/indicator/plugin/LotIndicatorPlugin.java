package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;

/**
 * Interface for the lot indicator plugin.
 */
public interface LotIndicatorPlugin extends IndicatorPlugin<MasterTenderLot> {
    /**
     * Takes data from source matched items, masters some fields of them and
     * returns result.
     *
     * @param lot
     *      lot to be evaluated
     * @param tender
     *      tender, context of the lot
     * @return calculated indicator or null if none
     */
    Indicator evaluate(MasterTenderLot lot, MasterTender tender);
}
