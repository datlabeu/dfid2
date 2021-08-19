package eu.dl.worker.master.plugin.specific;

import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;
import eu.dl.dataaccess.dto.matched.MatchedTender;
import eu.dl.worker.master.plugin.MasterPlugin;
import eu.dl.worker.utils.BasePlugin;

import java.util.List;

/**
 * This plugin sets the lot and bid ids of a master tender. After usage of this plugin, it supposes no changes in lots.
 */
public class LotAndBidIdsPlugin extends BasePlugin implements MasterPlugin<MatchedTender, MasterTender, MatchedTender> {

    @Override
    public final MasterTender master(final List<MatchedTender> matched, final MasterTender finalItem, final List<MatchedTender> context) {
        if (finalItem.getLots() == null) {
            return finalItem;
        }

        for (int i = 0; i < finalItem.getLots().size(); i++) {
            MasterTenderLot lot = finalItem.getLots().get(i);
            lot.setLotId(finalItem.getGroupId() + "_" + (i + 1));

            if (lot.getBids() != null) {
                for (int j = 0; j < lot.getBids().size(); j++) {
                    lot.getBids().get(j).setBidId(lot.getLotId() + "_" + (j + 1));
                }
            }
        }

        return finalItem;
    }
}
