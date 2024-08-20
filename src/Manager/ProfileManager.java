package Manager;

import Database.ConnectionEstablisher;

import java.sql.*;
import java.util.Map;

public class ProfileManager {

    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();

    public ProfileManager() {

    }

    /**
     * Defines a new investment profile with the given profile name and sector holdings.
     *
     * @param profileName    the name of the investment profile
     * @param sectorHoldings a map containing sector names as keys and their corresponding percentage holdings
     * @return true if the profile is successfully defined, false otherwise
     * @throws IllegalArgumentException if the total of sector holdings percentages is not equal to 100,
     *                                  or if one or more sectors in sectorHoldings are not valid
     */
    public boolean defineProfile(String profileName, Map<String, Integer> sectorHoldings) throws IllegalArgumentException {
        boolean success = false;
        // Check if the sum of sector holdings percentages equals 100
        int totalPercentage = sectorHoldings.values().stream().mapToInt(Integer::intValue).sum();
        try {
            if (totalPercentage != 100) {
                throw new IllegalArgumentException("The total of sector holdings percentages must equal 100.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return success;
        }
        try {
            if (!areAllSectorsValid(sectorHoldings)) {
                throw new IllegalArgumentException("One or more sectors in sectorHoldings are not valid.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return success;
        }

        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                // The rest of your method remains unchanged...
                // First, insert the profile
                String insertProfileSQL = "INSERT INTO Profiles (profileName) VALUES (?)";
                int profileId = 0;

                try (PreparedStatement insertProfileStmt = connection.prepareStatement(insertProfileSQL, Statement.RETURN_GENERATED_KEYS)) {
                    insertProfileStmt.setString(1, profileName);
                    int rowsAffected = insertProfileStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        try (ResultSet rs = insertProfileStmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                profileId = rs.getInt(1); // Retrieve the generated profile_id
                            }
                        }
                        success = true;
                    }
                }
                if (!sectorHoldings.containsKey("cash")) {
                    sectorHoldings.put("cash", 0);
                }
                // Then, insert sector holdings for this profile
                String insertHoldingsSQL = "INSERT INTO Profile_Sector_Holdings (profile_id, sector_id, percentage) VALUES (?, ?, ?)";
                try (PreparedStatement insertHoldingsStmt = connection.prepareStatement(insertHoldingsSQL)) {
                    for (Map.Entry<String, Integer> entry : sectorHoldings.entrySet()) {
                        int sectorId = getSectorIdByName(connection, entry.getKey());
                        if (sectorId != -1) {
                            insertHoldingsStmt.setInt(1, profileId);
                            insertHoldingsStmt.setInt(2, sectorId);
                            insertHoldingsStmt.setInt(3, entry.getValue());
                            insertHoldingsStmt.executeUpdate();
                        }
                    }
                    success = true;
                }
            } catch (SQLException e) {
                if (e.getSQLState().startsWith("23")) { // Unique constraint violation
                    System.out.println("profileName already exists: " + profileName);
                } else {
                    System.out.println("SQL error occurred: " + e.getMessage());
                }
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return success;

    }

    /**
     * Checks if all sectors in the provided sector holdings are valid.
     *
     * @param sectorHoldings a map containing sector names as keys and their corresponding percentage holdings
     * @return true if all sectors are valid, false otherwise
     */
    private boolean areAllSectorsValid(Map<String, Integer> sectorHoldings) {
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                for (String sectorName : sectorHoldings.keySet()) {
                    if (!isSectorValid(connection, sectorName)) {
                        return false;
                    }
                }
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return true;
    }

    /**
     * Checks if a sector with the given name exists in the database.
     *
     * @param connection the database connection
     * @param sectorName the name of the sector to check
     * @return true if the sector exists, false otherwise
     */
    private boolean isSectorValid(Connection connection, String sectorName) {
        String query = "SELECT COUNT(*) AS count FROM Sectors WHERE sectorName = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, sectorName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("count");
                return count > 0;
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves the ID of a sector by its name from the database.
     *
     * @param connection the database connection
     * @param sectorName the name of the sector
     * @return the ID of the sector if found, or -1 if not found
     * @throws SQLException if a database access error occurs
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

}
