package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;

/**
 * This plugin calculates funding info missing indicator.
 */
public class FundingsMissingIndicatorPlugin extends LotAverageIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_OR_INCOMPLETE_FUNDINGS_INFO.name();

    /**
     * Constructor sets lot level indicator plugin.
     */
    public FundingsMissingIndicatorPlugin() {
        super(new LotFundingsMissingIndicatorPlugin());
    }

    @Override
    public final String getType() {
        return TYPE;
    }
}
