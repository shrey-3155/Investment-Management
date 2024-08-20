package Manager;

import Database.ConnectionEstablisher;

import java.sql.*;

public class SectorManager {
    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();

    /**
     * Constructor to initialize the SectorManager and ensure the existence of the "cash" sector.
     */
    public SectorManager() {
        ensureCashSectorExists();
    }

    /**
     * Ensures that the "cash" sector exists in the database.
     */
    private void ensureCashSectorExists() {
        // Check if the "cash" sector exists
        Connection connection = connectionEstablisher.establishConnection();
        String checkSectorSQL = "SELECT COUNT(*) AS count FROM Sectors WHERE sectorName = 'cash'";
        try (PreparedStatement checkSectorStmt = connection.prepareStatement(checkSectorSQL)) {
            ResultSet rs = checkSectorStmt.executeQuery();
            if (rs.next() && rs.getInt("count") == 0) {
                // If not exists, insert the "cash" sector
                addSector("cash");
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred while ensuring 'cash' sector exists: " + e.getMessage());
        } finally {
            connectionEstablisher.closeConnection(connection);
        }
    }

    /**
     * Adds a new sector to the database.
     *
     * @param sectorName the name of the sector to add
     * @return true if the sector is successfully added, false otherwise
     */
    public boolean addSector(String sectorName) {
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            String insertSectorSQL = "INSERT INTO Sectors (sectorName) VALUES (?)";
            try (PreparedStatement insertSectorStmt = connection.prepareStatement(insertSectorSQL)) {
                insertSectorStmt.setString(1, sectorName);
                int rowsAffected = insertSectorStmt.executeUpdate();
                if (rowsAffected > 0) {
                    return true;
                }
            } catch (SQLException e) {
                if (e.getSQLState().startsWith("23")) { // Unique constraint violation
                    System.out.println("Sector already exists: " + sectorName);
                } else {
                    System.out.println("SQL error occurred: " + e.getMessage());
                }
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return false; // Return false if sector addition failed
    }
}


