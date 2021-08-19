package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.generic.Funding;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;

/**
 * Missing or incomplete award fundings info.
 */
public final class LotFundingsMissingIndicatorPlugin extends BaseLotKeyMissingIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_OR_INCOMPLETE_FUNDINGS_INFO.name();

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    protected PublicationFormType getFormType() {
        return PublicationFormType.CONTRACT_AWARD;
    }

    @Override
    protected Indicator getIndicator(final MasterTenderLot lot, final MasterTender tender) {
        IndicatorScore score = new IndicatorScore();
        List<Funding> fundings = ObjectUtils.firstNonNull(lot.getFundings(), tender.getFundings());
        if (fundings == null) {
            return undefined();
        }

        fundings.forEach(f -> score.test(f.getIsEuFund() != null));

        return calculated(score.ratio());
    }
}
