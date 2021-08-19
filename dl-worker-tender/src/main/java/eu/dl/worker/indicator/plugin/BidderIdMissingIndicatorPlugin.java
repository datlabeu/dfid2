package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;

/**
 * This plugin calculates bidder id missing indicator.
 */
public class BidderIdMissingIndicatorPlugin extends LotAverageIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_BIDDER_ID_MISSING.name();

    /**
     * Constructor sets lot level indicator plugin.
     */
    public BidderIdMissingIndicatorPlugin() {
        super(new LotBidderIdMissingIndicatorPlugin());
    }

    @Override
    public final String getType() {
        return TYPE;
    }
}
