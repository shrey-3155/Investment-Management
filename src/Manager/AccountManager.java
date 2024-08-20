package Manager;

import Database.ConnectionEstablisher;

import java.sql.*;

public class AccountManager {

    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();

    public AccountManager() {
    }

    /**
     * Creates a new account with the specified details.
     *
     * @param clientId           the ID of the client associated with the account
     * @param financialAdvisorId the ID of the financial advisor associated with the account
     * @param accountName        the name of the account
     * @param profileType        the type of profile associated with the account
     * @param reinvest           indicates whether the account reinvests
     * @return the ID of the newly created account, or -1 if creation fails
     * @throws SQLException if a database error occurs
     */
    public int createAccount(int clientId, int financialAdvisorId, String accountName, String profileType, boolean reinvest) throws SQLException {
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                // Validate client existence
                if (!clientExists(connection, clientId)) {
                    throw new SQLException("Client does not exist with ID: " + clientId);
                }

                // Validate advisor existence
                if (!advisorExists(connection, financialAdvisorId)) {
                    throw new SQLException("Advisor does not exist with ID: " + financialAdvisorId);
                }
                // Get the profile ID based on profileType name
                int profileId = getProfileIdByName(connection, profileType);
                if (profileId == -1) {
                    throw new SQLException("Profile type does not exist: " + profileType);
                }

                // Insert the new account
                String insertAccountSQL = "INSERT INTO Accounts (client_id, advisor_id, accountName, profile_id, reinvest) " +
                        "SELECT ?, ?, ?, ?, ? " +
                        "FROM DUAL " +
                        "WHERE NOT EXISTS (" +
                        "    SELECT 1 FROM Accounts WHERE client_id = ? AND accountName = ?" +
                        ")";
                try (PreparedStatement insertAccountStmt = connection.prepareStatement(insertAccountSQL, Statement.RETURN_GENERATED_KEYS)) {
                    insertAccountStmt.setInt(1, clientId);
                    insertAccountStmt.setInt(2, financialAdvisorId);
                    insertAccountStmt.setString(3, accountName);
                    insertAccountStmt.setInt(4, profileId);
                    insertAccountStmt.setBoolean(5, reinvest);
                    insertAccountStmt.setInt(6, clientId);
                    insertAccountStmt.setString(7, accountName);
                    int rowsAffected = insertAccountStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        try (ResultSet generatedKeys = insertAccountStmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int accountId = generatedKeys.getInt(1);
                                return accountId;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.out.println("SQL error occurred: " + e.getMessage());
                throw e;
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return -1; // Indicate failure
    }
    // Method to check if a client exists
    private boolean clientExists(Connection connection, int clientId) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Clients WHERE client_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }
        return false;
    }

    // Method to check if an advisor exists
    private boolean advisorExists(Connection connection, int financialAdvisorId) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Advisors WHERE advisor_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, financialAdvisorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }
        return false;
    }
    /**
     * Retrieves the profile ID associated with the given profile name.
     *
     * @param connection  the database connection
     * @param profileName the name of the profile
     * @return the ID of the profile, or -1 if the profile does not exist
     * @throws SQLException if a database error occurs
     */
    private int getProfileIdByName(Connection connection, String profileName) throws SQLException {
        String getProfileIdSQL = "SELECT profile_id FROM Profiles WHERE profileName = ?";
        try (PreparedStatement getProfileStmt = connection.prepareStatement(getProfileIdSQL)) {
            getProfileStmt.setString(1, profileName);
            try (ResultSet rs = getProfileStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("profile_id");
                } else {
                    return -1; // Profile not found
                }
            }
        }
    }

    /**
     * Retrieves the stock ID associated with the given stock symbol.
     *
     * @param connection  the database connection
     * @param stockSymbol the symbol of the stock
     * @return the ID of the stock, or -1 if the stock does not exist
     * @throws SQLException if a database error occurs
     */
    private int getStockIdByName(Connection connection, String stockSymbol) throws SQLException {
        String getProfileIdSQL = "SELECT stock_id FROM Stocks WHERE stockSymbol = ?";
        try (PreparedStatement getProfileStmt = connection.prepareStatement(getProfileIdSQL)) {
            getProfileStmt.setString(1, stockSymbol);
            try (ResultSet rs = getProfileStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock_id");
                } else {
                    return -1; // Profile not found
                }
            }
        }
    }

    /**
     * Initiates a share trading transaction for the specified account.
     *
     * @param accountId       the ID of the account involved in the transaction
     * @param stockSymbol     the symbol of the stock being traded, or "cash" for a cash transaction
     * @param sharesExchanged the number of shares being exchanged (positive for buying, negative for selling)
     * @return true if the transaction is successful, false otherwise
     */
    public boolean tradeShares(int accountId, String stockSymbol, int sharesExchanged) {
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                // Handle "cash" transaction separately
                if ("cash".equalsIgnoreCase(stockSymbol)) {
                    updateCashBalance(accountId, sharesExchanged, connection);
                    return true;
                }

                double sharePrice = getLastRecordedTradePrice(stockSymbol, connection);
                double transactionAmount = sharePrice * sharesExchanged;
                int stockId = getStockIdByName(connection, stockSymbol);

                if (stockId == -1) {
                    throw new SQLException("Stock symbol does not exist: " + stockId);
                }
                // Check if buying shares - must have enough cash in the account
                if (sharesExchanged > 0 && !hasSufficientCash(accountId, transactionAmount, connection)) {
                    System.out.println("Not enough cash in the account to complete the purchase.");
                    return false;
                }

                // Check if selling shares - must have the specified stock in the account
                if (sharesExchanged < 0 && !hasSufficientShares(accountId, stockId, Math.abs(sharesExchanged), connection)) {
                    System.out.println("Account does not have enough shares to sell.");
                    return false;
                }

                // Proceed with buying or selling shares
                updateCashBalance(accountId, -transactionAmount, connection); // Negative because buying shares decreases cash balance
                String transactionType = sharesExchanged > 0 ? "buy" : "sell";
                updateShareBalanceAndACB(accountId, stockId, Math.abs(sharesExchanged), sharePrice, transactionType, connection);
                return true;

            } catch (SQLException e) {
                System.out.println("SQL error occurred: " + e.getMessage());
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return false; // Return false if the method execution fails at any point
    }

    /**
     * Checks if the account has sufficient shares of the specified stock for selling.
     *
     * @param accountId    the ID of the account
     * @param stockId      the ID of the stock
     * @param sharesToSell the number of shares to sell
     * @param connection   the database connection
     * @return true if the account has sufficient shares, false otherwise
     * @throws SQLException if a database error occurs
     */
    private boolean hasSufficientShares(int accountId, int stockId, int sharesToSell, Connection connection) throws SQLException {
        String sql = "SELECT quantity FROM Investments WHERE account_id = ? AND stock_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            stmt.setInt(2, stockId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int quantity = rs.getInt("quantity");
                return quantity >= sharesToSell;
            }
        }
        return false; // Stock not found in account's holdings
    }

    /**
     * Checks if the account has sufficient cash for the specified amount.
     *
     * @param accountId    the ID of the account
     * @param amountNeeded the amount of cash needed for the transaction
     * @param connection   the database connection
     * @return true if the account has sufficient cash, false otherwise
     * @throws SQLException if a database error occurs
     */
    private boolean hasSufficientCash(int accountId, double amountNeeded, Connection connection) throws SQLException {
        String query = "SELECT cash_balance FROM Accounts WHERE account_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double cashBalance = rs.getDouble("cash_balance");
                return cashBalance >= amountNeeded;
            }
        }
        return false;
    }

    /**
     * Updates the share balance for the specified account and stock.
     *
     * @param accountId       the ID of the account
     * @param sharesExchanged the number of shares being exchanged
     * @param connection      the database connection
     * @param stock_id        the ID of the stock
     * @throws SQLException if a database error occurs
     */
    private void updateShareBalance(int accountId, int sharesExchanged, Connection connection, int stock_id) throws SQLException {
        String update = "INSERT INTO Investments (account_id,stock_id,quantity) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(update)) {
            stmt.setInt(1, accountId);
            stmt.setInt(2, stock_id);
            stmt.setInt(3, sharesExchanged);
            stmt.executeUpdate();
        }
    }

    /**
     * Updates the cash balance for the specified account.
     *
     * @param accountId  the ID of the account
     * @param amount     the amount to be added or subtracted from the cash balance
     * @param connection the database connection
     * @throws SQLException if a database error occurs
     */
    private void updateCashBalance(int accountId, double amount, Connection connection) throws SQLException {
        String update = "UPDATE Accounts SET cash_balance = cash_balance + ? WHERE account_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(update)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, accountId);
            stmt.executeUpdate();
        }
    }

    /**
     * Retrieves the last recorded trade price of the specified stock.
     *
     * @param stockSymbol the symbol of the stock
     * @param connection  the database connection
     * @return the last recorded trade price of the stock
     * @throws SQLException if a database error occurs
     */
    private double getLastRecordedTradePrice(String stockSymbol, Connection connection) throws SQLException {
        // Assuming there's a table "StockPrices" where prices are recorded
        String query = "SELECT perShare_price FROM Stocks WHERE stockSymbol = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, stockSymbol);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("perShare_price");
            } else {
                return 1.0; // Default price if no trades have occurred
            }
        }
    }

    /**
     * Updates the share balance and average cost basis (ACB) for the specified account and stock after a transaction.
     *
     * @param accountId       the ID of the account
     * @param stockId         the ID of the stock
     * @param sharesExchanged the number of shares being exchanged
     * @param pricePerShare   the price per share of the stock
     * @param transactionType the type of transaction (buy or sell)
     * @param connection      the database connection
     * @throws SQLException if a database error occurs
     */
    private void updateShareBalanceAndACB(int accountId, int stockId, int sharesExchanged, double pricePerShare, String transactionType, Connection connection) throws SQLException {
        // Check if an investment entry already exists for this account and stock
        String checkInvestmentSQL = "SELECT quantity, acb FROM Investments WHERE account_id = ? AND stock_id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkInvestmentSQL)) {
            checkStmt.setInt(1, accountId);
            checkStmt.setInt(2, stockId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                int existingQuantity = rs.getInt("quantity");
                double existingACB = rs.getDouble("acb");
                double updatedACB;

                if ("buy".equalsIgnoreCase(transactionType)) {
                    int newQuantity = existingQuantity + sharesExchanged;
                    // Calculate total cost of all shares before this purchase
                    double totalCostBeforePurchase = existingACB * existingQuantity;
                    // Calculate total cost of new purchase
                    double purchaseCost = pricePerShare * sharesExchanged;
                    // Update ACB to reflect new average
                    updatedACB = (totalCostBeforePurchase + purchaseCost) / newQuantity;
                    updateInvestment(accountId, stockId, newQuantity, updatedACB, connection); // Multiply ACB by newQuantity to store total cost, not per-share cost
                } else if ("sell".equalsIgnoreCase(transactionType)) {
                    int newQuantity = existingQuantity - sharesExchanged;
                    updateInvestment(accountId, stockId, newQuantity, existingACB, connection);
                }
            } else if ("buy".equalsIgnoreCase(transactionType)) {
                insertNewInvestment(accountId, stockId, sharesExchanged, pricePerShare, connection);
            }
            // Note: If selling but no existing investment found, throw an error or do nothing
        }
    }

    /**
     * Updates the investment entry for the specified account and stock.
     *
     * @param accountId   the ID of the account
     * @param stockId     the ID of the stock
     * @param newQuantity the new quantity of shares
     * @param newACB      the new average cost basis (ACB)
     * @param connection  the database connection
     * @throws SQLException if a database error occurs
     */
    private void updateInvestment(int accountId, int stockId, int newQuantity, double newACB, Connection connection) throws SQLException {
        String updateSQL = "UPDATE Investments SET quantity = ?, acb = ? WHERE account_id = ? AND stock_id = ?";
        try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
            updateStmt.setInt(1, newQuantity);
            updateStmt.setDouble(2, newACB);
            updateStmt.setInt(3, accountId);
            updateStmt.setInt(4, stockId);
            updateStmt.executeUpdate();
        }
    }

    /**
     * Inserts a new investment entry for the specified account and stock.
     *
     * @param accountId  the ID of the account
     * @param stockId    the ID of the stock
     * @param quantity   the quantity of shares
     * @param acb        the average cost basis (ACB)
     * @param connection the database connection
     * @throws SQLException if a database error occurs
     */
    private void insertNewInvestment(int accountId, int stockId, int quantity, double acb, Connection connection) throws SQLException {
        String insertSQL = "INSERT INTO Investments (account_id, stock_id, quantity, acb) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
            insertStmt.setInt(1, accountId);
            insertStmt.setInt(2, stockId);
            insertStmt.setInt(3, quantity);
            insertStmt.setDouble(4, acb);
            insertStmt.executeUpdate();
        }
    }

}
