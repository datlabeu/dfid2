package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.generic.CPV;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;


/**
 * Missing or incomplete notice cpvs.
 */
public final class LotCpvMissingIndicatorPlugin extends BaseLotKeyMissingIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_OR_INCOMPLETE_CPVS.name();

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    protected PublicationFormType getFormType() {
        return PublicationFormType.CONTRACT_NOTICE;
    }

    @Override
    protected Indicator getIndicator(final MasterTenderLot lot, final MasterTender tender) {
        IndicatorScore score = new IndicatorScore();

        List<CPV> cpvs = ObjectUtils.firstNonNull(lot.getCpvs(), tender.getCpvs());

        if (cpvs != null) {
            cpvs.forEach(n -> score.test(n.getCode() != null));
        }

        return calculated(score.ratio());
    }
}
