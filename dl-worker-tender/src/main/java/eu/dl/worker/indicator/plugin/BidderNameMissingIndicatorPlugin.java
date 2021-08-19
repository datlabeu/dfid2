package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;

/**
 * This plugin calculates bidder name missing indicator.
 */
public class BidderNameMissingIndicatorPlugin extends LotAverageIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_BIDDER_NAME_MISSING.name();

    /**
     * Constructor sets lot level indicator plugin.
     */
    public BidderNameMissingIndicatorPlugin() {
        super(new LotBidderNameMissingIndicatorPlugin());
    }

    @Override
    public final String getType() {
        return TYPE;
    }
}
