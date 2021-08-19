package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;

/**
 * This plugin calculates duration info missing indicator.
 */
public class DurationInfoMissingIndicatorPlugin extends LotAverageIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_OR_INCOMPLETE_DURATION_INFO.name();

    /**
     * Constructor sets lot level indicator plugin.
     */
    public DurationInfoMissingIndicatorPlugin() {
        super(new LotDurationInfoMissingIndicatorPlugin());
    }

    @Override
    public final String getType() {
        return TYPE;
    }
}

