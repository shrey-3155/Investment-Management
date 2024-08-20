package Reporting;

import Database.ConnectionEstablisher;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SectorWeights {

    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();

    public SectorWeights() {

    }

    /**
     * Retrieves the sector weights for a given account.
     *
     * @param accountId the ID of the account
     * @return a map containing sector names as keys and their corresponding weights as values
     */
    public Map<String, Integer> profileSectorWeights(int accountId) {
        Map<String, Double> sectorValues = new HashMap<>();
        double totalValue = 0.0;
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                if (!accountExists(connection, accountId)) {
                    throw new SQLException("Account with ID " + accountId + " does not exist.");
                }
                // Fetch market value for stocks by sector
                String query = "SELECT s.sector_id, sec.sectorName, SUM(i.quantity * s.perShare_price) AS sectorValue " +
                        "FROM Investments i " +
                        "JOIN Stocks s ON i.stock_id = s.stock_id " +
                        "JOIN Sectors sec ON s.sector_id = sec.sector_id " +
                        "WHERE i.account_id = ? " +
                        "GROUP BY s.sector_id";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, accountId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        String sectorName = rs.getString("sectorName");
                        double sectorValue = rs.getDouble("sectorValue");
                        sectorValues.put(sectorName, sectorValue);
                        totalValue += sectorValue;
                    }
                }

                // Add cash balance to the total account value
                double cashBalance = getCashBalance(accountId, connection);
                totalValue += cashBalance;
                sectorValues.put("cash", cashBalance);
                List<String> allSectors = getAllSectorNames(connection);

                // Calculate the percentage contribution of each sector
                Map<String, Integer> sectorPercentages = new HashMap<>();
                for (String sector : allSectors) {
                    if (!sectorValues.containsKey(sector)) {
                        sectorPercentages.put(sector, 0); // Sector not found in account, set percentage to 0
                    } else {
                        int percentage = (int) Math.round((sectorValues.get(sector) / totalValue) * 100);
                        sectorPercentages.put(sector, percentage);
                    }
                }

                return sectorPercentages;
            } catch (SQLException e) {
                System.out.println("SQL error occurred: " + e.getMessage());
            } finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    System.out.println("Error closing connection: " + e.getMessage());
                }
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return new HashMap<>(); // Return an empty map in case of failure
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
     * Retrieves the names of all sectors from the database.
     *
     * @param connection the database connection
     * @return a list containing the names of all sectors
     * @throws SQLException if an SQL error occurs
     */
    private List<String> getAllSectorNames(Connection connection) throws SQLException {
        List<String> sectors = new ArrayList<>();
        String query = "SELECT sectorName FROM Sectors";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                sectors.add(rs.getString("sectorName"));
            }
        }
        return sectors;
    }

    /**
     * Retrieves the cash balance for a given account.
     *
     * @param accountId  the ID of the account
     * @param connection the database connection
     * @return the cash balance of the account
     * @throws SQLException if an SQL error occurs
     */
    private double getCashBalance(int accountId, Connection connection) throws SQLException {
        String query = "SELECT cash_balance FROM Accounts WHERE account_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("cash_balance");
            }
        }
        return 0.0; // Default to 0 if not found
    }
}
