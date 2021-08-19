package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;

/**
 * This plugin calculates Tax haven indicator.
 */
public class TaxHavenIndicatorPlugin extends LotAverageIndicatorPlugin {

    /**
     * Constructor sets lot level indicator plugin.
     */
    public TaxHavenIndicatorPlugin() {
        super(new LotTaxHavenIndicatorPlugin());
    }

    @Override
    public final String getType() {
        return TenderIndicatorType.INTEGRITY_TAX_HAVEN.name();
    }

}