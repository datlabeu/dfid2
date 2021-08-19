package eu.dl.dataaccess.dao.jdbc;

import eu.dl.core.UnrecoverableException;
import eu.dl.core.config.Config;
import eu.dl.dataaccess.dao.TenderAdministratorsDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * JDBC implementation of administrators blacklist DAO.
 */
public final class JdbcTenderAdministratorsDAO implements TenderAdministratorsDAO {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected Config config;

    protected String schema;

    /**
     * Transaction utils used to provide connection etc.
     */
    private static final JdbcTransactionUtils transactionUtils = JdbcTransactionUtils.getInstance();

    private static final String TABLE_NAME = "tender_administrators";

    /**
     * Initializes connection etc.
     */
    public JdbcTenderAdministratorsDAO() {
        config = Config.getInstance();

        schema = config.getParam("jdbc.schema");

    }

    /**
     * @return table name with schema
     */
    private String getTableWithSchema() {
        return schema + "." + TABLE_NAME;
    }

    @Override
    public boolean isInBlackList(final String email) {
        try {
            PreparedStatement statement = transactionUtils.getConnection().prepareStatement(
                    "SELECT *" +
                            " FROM " + getTableWithSchema() +
                            " WHERE buyer_email = ?");

            statement.setString(1, email);

            ResultSet rs = statement.executeQuery();
            // get the first record
            if (rs.next()) {
                rs.close();
                statement.close();
                return true;
            }

            rs.close();
            statement.close();
            return false;
        } catch (Exception e) {
            logger.error("Unable to perform query, because of {}", e);
            throw new UnrecoverableException("Unable to perform query.", e);
        }
    }
}
