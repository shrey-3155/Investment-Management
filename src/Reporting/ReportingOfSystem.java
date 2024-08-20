package Reporting;

import Database.ConnectionEstablisher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportingOfSystem {

    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();

    public ReportingOfSystem() {

    }

    /**
     * Calculates the total market value of an account.
     *
     * @param accountId the ID of the account
     * @return the total market value of the account
     */
    public double accountValue(int accountId) {
        double totalValue = 0.0;
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                // Check if the account ID exists
                if (!accountExists(connection, accountId)) {
                    throw new SQLException("Account with ID " + accountId + " does not exist.");
                }

                // Calculate the total value of investments
                String queryInvestments =
                        "SELECT i.quantity, s.perShare_price " +
                                "FROM Investments i " +
                                "JOIN Stocks s ON i.stock_id = s.stock_id " +
                                "WHERE i.account_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(queryInvestments)) {
                    stmt.setInt(1, accountId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        int quantity = rs.getInt("quantity");
                        double sharePrice = rs.getDouble("perShare_price");
                        totalValue += quantity * sharePrice; // Add the value of this investment to the total
                    }
                }

                // Add the cash balance of the account to the total value
                String queryCashBalance = "SELECT cash_balance FROM Accounts WHERE account_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(queryCashBalance)) {
                    stmt.setInt(1, accountId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        double cashBalance = rs.getDouble("cash_balance");
                        totalValue += cashBalance; // Add cash balance to the total value
                    }
                }
            } catch (SQLException e) {
                System.out.println("SQL error occurred: " + e.getMessage());
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return totalValue; // Return the total market value of the account
    }

    /**
     * Checks if an account exists.
     *
     * @param connection the database connection
     * @param accountId  the ID of the account
     * @return true if the account exists, otherwise false
     * @throws SQLException if an SQL error occurs
     */
    private boolean accountExists(Connection connection, int accountId) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Accounts WHERE account_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("count");
                return count > 0;
            }
        }
        return false;
    }

    /**
     * Calculates the total portfolio value managed by an advisor.
     *
     * @param advisorId the ID of the advisor
     * @return the total portfolio value managed by the advisor
     */
    public double advisorPortfolioValue(int advisorId) {
        double totalPortfolioValue = 0.0;
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                // Check if the advisor ID exists
                if (!advisorExists(connection, advisorId)) {
                    throw new SQLException("Advisor with ID " + advisorId + " does not exist.");
                }

                // Step 1: Fetch all account IDs managed by the specified advisor.
                String queryAccounts = "SELECT account_id FROM Accounts WHERE advisor_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(queryAccounts)) {
                    stmt.setInt(1, advisorId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        int accountId = rs.getInt("account_id");

                        // Step 2: Calculate the market value for each account.
                        double accountValue = accountValue(accountId);

                        // Step 3: Sum these values.
                        totalPortfolioValue += accountValue;
                    }
                }
            } catch (SQLException e) {
                System.out.println("SQL error occurred: " + e.getMessage());
                return -1.0; // Indicate an error condition
            } finally {
               connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
            return -1.0; // Indicate an error condition
        }
        return totalPortfolioValue;
    }

    /**
     * Checks if an advisor exists.
     *
     * @param connection the database connection
     * @param advisorId  the ID of the advisor
     * @return true if the advisor exists, otherwise false
     * @throws SQLException if an SQL error occurs
     */
    private boolean advisorExists(Connection connection, int advisorId) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Advisors WHERE advisor_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, advisorId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("count");
                return count > 0;
            }
        }
        return false;
    }

    /**
     * Calculates the profit for each account managed by a client.
     *
     * @param clientId the ID of the client
     * @return a map of account IDs to their respective profits
     */
    public Map<Integer, Double> investorProfit(int clientId) {
        Map<Integer, Double> accountProfits = new HashMap<>(); // Account ID to profit mapping
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                // Check if the client ID exists
                if (!clientExists(connection, clientId)) {
                    throw new SQLException("Client with ID " + clientId + " does not exist.");
                }

                // Step 1: Fetch all account IDs for the given client ID
                List<Integer> accountIds = getAccountIdsForClient(clientId, connection);
                for (Integer accountId : accountIds) {
                    double totalProfit = 0.0;
                    // Fetch all investments for the account
                    String query = "SELECT i.stock_id, i.quantity, i.acb, s.perShare_price FROM Investments i JOIN Stocks s ON i.stock_id = s.stock_id WHERE i.account_id = ?";
                    try (PreparedStatement stmt = connection.prepareStatement(query)) {
                        stmt.setInt(1, accountId);
                        ResultSet rs = stmt.executeQuery();
                        while (rs.next()) {
                            int quantity = rs.getInt("quantity");
                            double acb = rs.getDouble("acb");
                            double currentPrice = rs.getDouble("perShare_price");
                            double currentMarketValue = quantity * currentPrice;
                            double costBase = acb * quantity; // This assumes acb is stored as total cost, not per-share cost
                            double profit = currentMarketValue - costBase;
                            totalProfit += profit;
                        }
                    }
                    accountProfits.put(accountId, totalProfit);
                }
            } catch (SQLException e) {
                System.out.println("SQL error occurred: " + e.getMessage());
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return accountProfits;
    }

    /**
     * Checks if a client exists.
     *
     * @param connection the database connection
     * @param clientId   the ID of the client
     * @return true if the client exists, otherwise false
     * @throws SQLException if an SQL error occurs
     */
    private boolean clientExists(Connection connection, int clientId) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Clients WHERE client_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("count");
                return count > 0;
            }
        }
        return false;
    }

    /**
     * Retrieves all account IDs associated with a client.
     *
     * @param clientId   the ID of the client
     * @param connection the database connection
     * @return a list of account IDs associated with the client
     * @throws SQLException if an SQL error occurs
     */
    private List<Integer> getAccountIdsForClient(int clientId, Connection connection) throws SQLException {
        List<Integer> accountIds = new ArrayList<>();
        String query = "SELECT account_id FROM Accounts WHERE client_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                accountIds.add(rs.getInt("account_id"));
            }
        }
        return accountIds;
    }


}
