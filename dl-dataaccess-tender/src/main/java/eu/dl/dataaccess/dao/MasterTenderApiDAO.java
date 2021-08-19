package eu.dl.dataaccess.dao;

/**
 * Master tender DAO for API purposes. Combines matching and opentender support.
 */
public interface MasterTenderApiDAO extends MasterTenderMatchingDAO, MasterTenderOpentenderDAO {
    /**
     * Returns an array of two values. The first value (index 0) is a number of tenders where the given group id occurs as a buyer,
     * the second value is the number of tenders where the body is a supplier.
     *
     * @param groupId
     *      body group id
     * @return array of two values
     */
    int[] getBodyGroupIdCounts(String groupId);
}
