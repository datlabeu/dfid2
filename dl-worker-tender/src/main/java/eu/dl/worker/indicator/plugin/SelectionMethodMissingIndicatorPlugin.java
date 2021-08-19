package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;

/**
 * This plugin calculates selection method missing indicator.
 */
public class SelectionMethodMissingIndicatorPlugin extends LotAverageIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_SELECTION_METHOD.name();

    /**
     * Constructor sets lot level indicator plugin.
     */
    public SelectionMethodMissingIndicatorPlugin() {
        super(new LotSelectionMethodMissingIndicatorPlugin());
    }

    @Override
    public final String getType() {
        return TYPE;
    }
}
