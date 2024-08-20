package Manager;

import Database.ConnectionEstablisher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StockManager {
    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();

    public StockManager() {

    }

    /**
     * Adds a new stock to the database.
     *
     * @param companyName the name of the company
     * @param stockSymbol the symbol of the stock
     * @param sectorName  the name of the sector to which the stock belongs
     * @return true if the stock is successfully added, false otherwise
     */

    public boolean addStock(String companyName, String stockSymbol, String sectorName) {
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                // First, get the sector ID based on the sector name
                int sectorId = getSectorIdByName(connection, sectorName);
                if (sectorId == -1) {
                    System.out.println("Sector does not exist: " + sectorName);
                    return false;
                }
                String insertStockSQL = "INSERT INTO Stocks (stockName, stockSymbol, sector_id) VALUES (?, ?, ?)";

                try (PreparedStatement insertStockStmt = connection.prepareStatement(insertStockSQL)) {
                    insertStockStmt.setString(1, companyName);
                    insertStockStmt.setString(2, stockSymbol);
                    insertStockStmt.setInt(3, sectorId);
                    int rowsAffected = insertStockStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        return true;
                    }
                }
            } catch (SQLException e) {
                if (e.getSQLState().startsWith("23")) {
                    System.out.println("Stock already exists: " + companyName + " (" + stockSymbol + ")");
                } else {
                    System.out.println("SQL error occurred: " + e.getMessage());
                }
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return false; // Return false if stock addition failed
    }

    /**
     * Retrieves the sector ID based on the sector name.
     *
     * @param connection the database connection
     * @param sectorName the name of the sector
     * @return the sector ID if found, -1 otherwise
     * @throws SQLException if a SQL error occurs
     */
    private int getSectorIdByName(Connection connection, String sectorName) throws SQLException {
        String getSectorIdSQL = "SELECT sector_id FROM Sectors WHERE sectorName = ?";
        try (PreparedStatement getSectorStmt = connection.prepareStatement(getSectorIdSQL)) {
            getSectorStmt.setString(1, sectorName);
            try (ResultSet rs = getSectorStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("sector_id");
                } else {
                    return -1; // Sector not found
                }
            }
        }
    }

    /**
     * Sets the price per share for a stock.
     *
     * @param stockSymbol   the symbol of the stock
     * @param perSharePrice the price per share to set
     * @return true if the stock price is successfully updated, false otherwise
     */
    public boolean setStockPrice(String stockSymbol, double perSharePrice) throws SQLException {
        Connection connection = connectionEstablisher.establishConnection();

        if (connection != null) {
            // Check if the stock symbol exists
            boolean stockExists = false;
            String checkStockExistsSQL = "SELECT COUNT(*) FROM Stocks WHERE stockSymbol = ?";

            try (PreparedStatement checkStockStmt = connection.prepareStatement(checkStockExistsSQL)) {
                checkStockStmt.setString(1, stockSymbol);
                ResultSet resultSet = checkStockStmt.executeQuery();
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    stockExists = (count > 0);
                }
            } catch (SQLException e) {
                return false; // Return false if an error occurs during validation
            }

            if (!stockExists) {
                throw new SQLException("Stock does not exist: " + stockSymbol);
            }
            String updateStockPriceSQL = "UPDATE Stocks SET perShare_price = ? WHERE stockSymbol = ?";

            try (PreparedStatement updateStockStmt = connection.prepareStatement(updateStockPriceSQL)) {
                updateStockStmt.setDouble(1, perSharePrice);
                updateStockStmt.setString(2, stockSymbol);

                int rowsAffected = updateStockStmt.executeUpdate();
                if (rowsAffected > 0) {
                    return true;
                } else {
                    return false;
                }
            } catch (SQLException e) {
                System.out.println("SQL error occurred: " + e.getMessage());
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return false; // Return false if database connection failed
    }



}
