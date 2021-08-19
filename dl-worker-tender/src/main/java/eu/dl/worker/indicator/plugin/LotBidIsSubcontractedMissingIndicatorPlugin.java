package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;
import eu.dl.dataaccess.dto.master.MasterBid;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;

import java.util.Objects;

/**
 * Missing award subcontracting info.
 */
public final class LotBidIsSubcontractedMissingIndicatorPlugin extends BaseLotKeyMissingIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_SUBCONTRACTED_INFO.name();

    @Override
    protected PublicationFormType getFormType() {
        return PublicationFormType.CONTRACT_AWARD;
    }

    @Override
    protected Indicator getIndicator(final MasterTenderLot lot, final MasterTender tender) {
        if (tender == null || lot == null || lot.getBids() == null || lot.getBids().isEmpty()) {
            return insufficient();
        }

        MasterBid winningBid = lot.getBids().stream().filter(Objects::nonNull)
            .filter(a -> a.getIsWinning() != null && a.getIsWinning()).findFirst().orElse(null);

        if (winningBid == null) {
            return insufficient();
        }

        IndicatorScore score = new IndicatorScore();
        score.test(winningBid.getIsSubcontracted() != null);

        return calculated(score.ratio());
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
