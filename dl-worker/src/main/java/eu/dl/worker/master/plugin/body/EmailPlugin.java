package eu.dl.worker.master.plugin.body;

import eu.dl.dataaccess.dao.jdbc.JdbcTenderAdministratorsDAO;
import eu.dl.dataaccess.dto.master.MasterBody;
import eu.dl.dataaccess.dto.matched.MatchedBody;
import eu.dl.worker.master.plugin.MasterPlugin;
import eu.dl.worker.utils.BasePlugin;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Plugin for mastering emails.
 *
 * @param <T>
 *            matched items type
 * @param <V>
 *            item type to be mastered
 */
public class EmailPlugin<T extends MatchedBody, V> extends BasePlugin implements MasterPlugin<T, V, Object> {

    /**
     * Plugin name.
     */
    public static final String PLUGIN_ID = "email";
    private final JdbcTenderAdministratorsDAO administratorsDAO;

    private final Map<String, Boolean> checkedAdministrators;


    /**
     * No empty constructor allowed.
     */
    public EmailPlugin() {
        // no empty constructor allowed.
        checkedAdministrators = new HashMap<>();
        administratorsDAO = new JdbcTenderAdministratorsDAO();
    }

    /**
     * Checks if given email belongs to administrator. Uses map to save checked emails.
     * @param email email
     * @return true if found in administrators blacklist, false otherwise
     */
    private boolean isAdministrator(final String email) {
        if (checkedAdministrators.containsKey(email)) {
            return checkedAdministrators.get(email);
        }
        boolean isAdministrator = administratorsDAO.isInBlackList(email);
        checkedAdministrators.put(email, isAdministrator);
        return isAdministrator;
    }

    /**
     * Checks if email is too old - the last seen is older than 3 years from now.
     * @param items matched bodies
     * @param email email to check
     * @return true if email is too old, false otherwise
     */
    private boolean isTooOld(final List<MatchedBody> items, final String email) {
        LocalDate lastSeen = items.stream()
                .filter(item -> item.getEmail().equals(email))
                .map(MatchedBody::getPublicationDate).filter(Objects::nonNull)
                .max(LocalDate::compareTo).orElse(null);

        if (lastSeen == null) {
            return true;
        }

        // if last seen is more than 3 years before now, the email is too old
        return lastSeen.compareTo(LocalDate.now().minusYears(3)) < 0;

    }

    @Override
    public final V master(final List<T> items, final V finalItem, final List<Object> context) {
        // filter only bodies with email
        List<MatchedBody> itemsWithEmail = items.stream().filter(Objects::nonNull)
                .filter(item -> item.getEmail() != null && !item.getEmail().isEmpty()).collect(Collectors.toList());
        itemsWithEmail.stream().filter(item -> item.getPublicationDate() == null)
                .forEach(item -> item.setPublicationDate(LocalDate.of(2000, 1, 1)));

        if (itemsWithEmail.isEmpty()) {
            return finalItem;
        }

        // sort by publication date
        itemsWithEmail.sort(Comparator.comparing(MatchedBody::getPublicationDate).reversed());

        MasterBody masterBody = (MasterBody) finalItem;
        // map for counting unique publications where emails occur
        HashMap<String, Set<String>> cleanPublicationIdsForEmails = new HashMap<>();

        for (MatchedBody item : itemsWithEmail) {
            if (!cleanPublicationIdsForEmails.containsKey(item.getEmail())) {
                cleanPublicationIdsForEmails.put(item.getEmail(), new HashSet<>());
            }
            Set<String> publications = cleanPublicationIdsForEmails.get(item.getEmail());
            publications.add(item.getCleanObjectId());
            // if email was seen three times and is not administrator and is not too old than return this email
            if (publications.size() == 3) {
                if (!isAdministrator(item.getEmail()) && !isTooOld(itemsWithEmail, item.getEmail())) {
                    masterBody.setEmail(item.getEmail());
                    return finalItem;
                }
            }
        }

        // if nothing fits get the most recent email
        logger.info("No emails matching plugin conditions, use the most recent email");
        masterBody.setEmail(itemsWithEmail.get(0).getEmail());
        return finalItem;
    }

}
