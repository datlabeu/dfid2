package eu.dl.dataaccess.dao;

/**
 * Administrators blacklist DAO interface.
 */
public interface TenderAdministratorsDAO {

    /**
     * Checks if given email is in blacklist (in materialised view tender_administrators).
     * @param email email
     * @return true if email is in blacklist, false otherwise
     */
    boolean isInBlackList(final String email);
}
