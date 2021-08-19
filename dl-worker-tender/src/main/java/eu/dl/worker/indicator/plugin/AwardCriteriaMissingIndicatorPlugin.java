package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;

/**
 * This plugin calculates award criteria missing indicator.
 */
public class AwardCriteriaMissingIndicatorPlugin  extends LotAverageIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_OR_INCOMPLETE_AWARD_CRITERIA.name();

    /**
     * Constructor sets lot level indicator plugin.
     */
    public AwardCriteriaMissingIndicatorPlugin() {
        super(new LotAwardCriteriaMissingIndicatorPlugin());
    }

    @Override
    public final String getType() {
        return TYPE;
    }
}
