package Manager;

import Database.ConnectionEstablisher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DividendManager {
    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();
    AccountManager accountManager = new AccountManager();

    public DividendManager() {
    }

    /**
     * Disburses dividends to client accounts and the firm for a given stock.
     *
     * @param stockSymbol      the symbol of the stock for which dividends are disbursed
     * @param dividendPerShare the dividend amount per share
     * @return the number of fractional shares added to the firm due to dividend reinvestment
     */
    public int disburseDividend(String stockSymbol, double dividendPerShare) {
        double sharesToBuyForCompany = 0.0;
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                int stockId = fetchStockId(stockSymbol, connection);
                if (stockId == -1) {
                    throw new SQLException("Stock Symbol does not exist");
                }

                // Process dividends for each account holding this stock
                String sql = "SELECT i.account_id, i.quantity, a.reinvest " +
                        "FROM Investments i " +
                        "JOIN Accounts a ON i.account_id = a.account_id " +
                        "WHERE i.stock_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, stockId);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        int accountId = rs.getInt("account_id");
                        double quantity = rs.getDouble("quantity");
                        boolean reinvest = rs.getBoolean("reinvest");
                        double totalDividend = quantity * dividendPerShare;
                        double currentPricePerShare = fetchCurrentPricePerShare(stockId, connection);
                        double sharesToBuy = totalDividend / currentPricePerShare;
                        if (reinvest) {

                            int wholeSharesToBuy = (int) sharesToBuy; // Extract whole part
                            double fractionalSharesToBuy = sharesToBuy - wholeSharesToBuy; // Extract fractional part
                            sharesToBuyForCompany += fractionalSharesToBuy;

                            // Buy whole shares
                            purchaseSharesForAccount(accountId, stockId, sharesToBuy, connection);
                            // Manage fractional shares for the firm
                        } else {
                            // Update cash balance directly if not reinvesting
                            updateCashBalance(accountId, totalDividend, connection);
                        }
                    }
                    return manageFirmFractionalShares(stockId, sharesToBuyForCompany, connection);
                }

            } catch (SQLException e) {
                System.out.println("SQL error occurred: " + e.getMessage());
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return 0;
    }

    /**
     * Fetches the stock ID for a given stock symbol from the database.
     *
     * @param stockSymbol the symbol of the stock
     * @param connection  the database connection
     * @return the ID of the stock, or -1 if the stock is not found
     * @throws SQLException if a database access error occurs
     */
    private int fetchStockId(String stockSymbol, Connection connection) throws SQLException {
        String sql = "SELECT stock_id FROM Stocks WHERE stockSymbol = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, stockSymbol);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("stock_id");
            }
        }
        return -1; // Return an invalid ID if stock not found
    }

    /**
     * Fetches the current price per share for a given stock ID from the database.
     *
     * @param stockId    the ID of the stock
     * @param connection the database connection
     * @return the current price per share of the stock, or 1.0 if not found
     * @throws SQLException if a database access error occurs
     */
    private double fetchCurrentPricePerShare(int stockId, Connection connection) throws SQLException {
        String sql = "SELECT perShare_price FROM Stocks WHERE stock_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, stockId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("perShare_price");
            }
        }
        return 1.0; // Default price if not found
    }

    /**
     * Purchases shares for a specific account and updates the database accordingly.
     *
     * @param accountId   the ID of the account
     * @param stockId     the ID of the stock
     * @param sharesToBuy the number of shares to purchase
     * @param connection  the database connection
     * @throws SQLException if a database access error occurs
     */
    private void purchaseSharesForAccount(int accountId, int stockId, double sharesToBuy, Connection connection) throws SQLException {
        // Assuming an Investments table that tracks the number of shares per account per stock
        String sql = "INSERT INTO Investments (account_id, stock_id, quantity) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quantity = quantity + ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            stmt.setInt(2, stockId);
            stmt.setDouble(3, sharesToBuy);
            stmt.setDouble(4, sharesToBuy); // For the UPDATE case
            stmt.executeUpdate();
        }
    }

    /**
     * Manages fractional shares for the firm and updates the database accordingly.
     *
     * @param stockId               the ID of the stock
     * @param fractionalSharesToAdd the number of fractional shares to add
     * @param connection            the database connection
     * @return the number of whole shares added to the firm
     * @throws SQLException if a database access error occurs
     */
    private int manageFirmFractionalShares(int stockId, double fractionalSharesToAdd, Connection connection) throws SQLException {
        String sql = "SELECT stocks_owned FROM FirmStockHoldings WHERE stock_id = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(sql)) {
            selectStmt.setInt(1, stockId);
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                double sharesOwned = rs.getDouble("stocks_owned");

                // Update the firm's holdings
                String updateSql = "UPDATE FirmStockHoldings SET stocks_owned = ? WHERE stock_id = ?";
                if (sharesOwned < fractionalSharesToAdd) {
                    double returnValue = sharesOwned + Math.floor(fractionalSharesToAdd - sharesOwned) + 1 - fractionalSharesToAdd;
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setDouble(1, returnValue);
                        updateStmt.setInt(2, stockId);
                        updateStmt.executeUpdate();
                    }
                    return (int) Math.floor(fractionalSharesToAdd - sharesOwned) + 1;
                } else {
                    double returnValue = sharesOwned - fractionalSharesToAdd;
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setDouble(1, returnValue);
                        updateStmt.setInt(2, stockId);
                        updateStmt.executeUpdate();
                    }
                    return 0;
                }
            } else {
                // Insert new record if not exists
                String insertSql = "INSERT INTO FirmStockHoldings (stock_id, stocks_owned) VALUES (?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, stockId);
                    insertStmt.setDouble(2, fractionalSharesToAdd); // Whole share if fractionalSharesToAdd >= 1
                    insertStmt.executeUpdate();
                }
                return (int) Math.floor(fractionalSharesToAdd) + 1;
            }
        }
    }

    /**
     * Updates the cash balance of an account in the database.
     *
     * @param accountId  the ID of the account
     * @param amount     the amount to update the cash balance by
     * @param connection the database connection
     * @throws SQLException if a database access error occurs
     */
    private void updateCashBalance(int accountId, double amount, Connection connection) throws SQLException {
        String sql = "UPDATE Accounts SET cash_balance = cash_balance + ? WHERE account_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, accountId);
            int updatedRows = stmt.executeUpdate();
            if (updatedRows > 0) {
                System.out.println("Cash balance updated successfully for account ID: " + accountId);
            } else {
                System.out.println("Failed to update cash balance for account ID: " + accountId);
            }
        }
    }


}
