package eu.dl.worker.indicator.plugin;

import eu.dl.dataaccess.dto.codetables.PublicationFormType;
import eu.dl.dataaccess.dto.codetables.SelectionMethod;
import eu.dl.dataaccess.dto.generic.AwardCriterion;
import eu.dl.dataaccess.dto.indicator.Indicator;
import eu.dl.dataaccess.dto.indicator.TenderIndicatorType;
import eu.dl.dataaccess.dto.master.MasterTender;
import eu.dl.dataaccess.dto.master.MasterTenderLot;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;


/**
 * Missing or incomplete notice award criteria.
 */
public final class LotAwardCriteriaMissingIndicatorPlugin extends BaseLotKeyMissingIndicatorPlugin {
    /**
     * Plugin type.
     */
    public static final String TYPE = TenderIndicatorType.TRANSPARENCY_MISSING_OR_INCOMPLETE_AWARD_CRITERIA.name();

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
        // insufficient data
        if (tender.getSelectionMethod() != SelectionMethod.MEAT) {
            return undefined();
        }

        List<AwardCriterion> criteria = ObjectUtils.firstNonNull(lot.getAwardCriteria(), tender.getAwardCriteria());

        IndicatorScore score = new IndicatorScore();

        if (criteria != null) {
            criteria.forEach(n -> {
                score.test(n.getName() != null);
                score.test(n.getWeight() != null);
            });
        }

        return calculated(score.ratio());
    }
}
