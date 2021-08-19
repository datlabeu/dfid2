package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;

/**
 * This plugin calculates subcontracted info missing indicator.
 */
public class BidIsSubcontractedMissingIndicatorPlugin extends LotAverageIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_SUBCONTRACTED_INFO.name();

    /**
     * Constructor sets lot level indicator plugin.
     */
    public BidIsSubcontractedMissingIndicatorPlugin() {
        super(new LotBidIsSubcontractedMissingIndicatorPlugin());
    }

    @Override
    public final String getType() {
        return TYPE;
    }
}
