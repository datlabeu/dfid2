package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;

/**
 * This plugin calculates value missing indicator.
 */
public class ValueMissingIndicatorPlugin extends LotAverageIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_VALUE_MISSING.name();

    /**
     * Constructor sets lot level indicator plugin.
     */
    public ValueMissingIndicatorPlugin() {
        super(new LotValueMissingIndicatorPlugin());
    }

    @Override
    public final String getType() {
        return TYPE;
    }
}
