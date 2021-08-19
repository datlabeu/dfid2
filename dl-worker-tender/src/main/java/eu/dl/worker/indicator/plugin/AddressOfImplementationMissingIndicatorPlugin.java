package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;
import eu.dl.dataaccess.dto.master.MasterTender;

import java.util.List;

/**
 * Missing nuts of award address of implementation.
 */
public final class AddressOfImplementationMissingIndicatorPlugin extends BaseKeyMissingIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_ADDRESS_OF_IMPLEMENTATION_NUTS.name();

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    protected PublicationFormType getFormType() {
        return PublicationFormType.CONTRACT_AWARD;
    }

    @Override
    protected Indicator getIndicator(final MasterTender tender) {
        IndicatorScore score = new IndicatorScore();

        if (tender.getAddressOfImplementation() != null) {
            List<String> nuts = tender.getAddressOfImplementation().getNuts();
            score.test(nuts != null && !nuts.isEmpty());
        }

        return calculated(score.ratio());
    }
}
