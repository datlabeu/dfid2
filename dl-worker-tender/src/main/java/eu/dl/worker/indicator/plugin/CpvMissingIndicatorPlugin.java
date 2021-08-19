package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;

/**
 * This plugin calculates CPVs missing indicator.
 */
public class CpvMissingIndicatorPlugin extends LotAverageIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_OR_INCOMPLETE_CPVS.name();

    /**
     * Constructor sets lot level indicator plugin.
     */
    public CpvMissingIndicatorPlugin() {
        super(new LotCpvMissingIndicatorPlugin());
    }

    @Override
    public final String getType() {
        return TYPE;
    }
}
