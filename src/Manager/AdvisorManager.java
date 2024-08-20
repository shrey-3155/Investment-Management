package Manager;

import Database.ConnectionEstablisher;

import java.sql.*;

public class AdvisorManager {

    /**
     * The connection establisher used for connecting to the database.
     */
    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();

    public AdvisorManager() {
    }

    /**
     * Adds a new advisor with the specified name to the database.
     *
     * @param advisorName the name of the advisor to add
     * @return the ID of the newly added advisor, or -1 if addition fails
     */
    public int addAdvisor(String advisorName) {
        Connection connection = connectionEstablisher.establishConnection();

        if (connection != null) {
            String insertAdvisorSQL = "INSERT INTO Advisors (advisorName) VALUES (?)";
            try (PreparedStatement insertAdvisorStmt = connection.prepareStatement(insertAdvisorSQL, Statement.RETURN_GENERATED_KEYS)) {
                insertAdvisorStmt.setString(1, advisorName);

                int rowsAffected = insertAdvisorStmt.executeUpdate();
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = insertAdvisorStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int advisorId = generatedKeys.getInt(1); // Obtain the generated advisor ID
                            return advisorId;
                        } else {
                            throw new SQLException("Creating advisor failed, no ID obtained.");
                        }
                    }
                } else {
                    return -1; // Indicate failure
                }
            } catch (SQLException e) {
                if (e.getSQLState().startsWith("23")) { // Unique constraint violation
                    System.out.println("Advisor already exists: " + advisorName);
                } else {
                    System.out.println("SQL error occurred: " + e.getMessage());
                }
                return -1; // Indicate failure
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            return -1; // Indicate failure
        }
    }

    /**
     * Changes the advisor associated with a specified account.
     *
     * @param accountId    the ID of the account for which to change the advisor
     * @param newAdvisorId the ID of the new advisor
     * @return true if the advisor change is successful, false otherwise
     */
    public boolean changeAdvisor(int accountId, int newAdvisorId) {
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                if (!advisorExists(connection, accountId) || !advisorExists(connection, newAdvisorId)) {
                    throw new SQLException("One or both of the provided advisor IDs do not exist.");
                }

                // SQL statement to update the advisor for a given account
                String updateAdvisorSQL = "UPDATE Accounts SET advisor_id = ? WHERE account_id = ?";

                // Prepare the statement with the new advisor ID and account ID
                try (PreparedStatement updateAdvisorStmt = connection.prepareStatement(updateAdvisorSQL)) {
                    updateAdvisorStmt.setInt(1, newAdvisorId);
                    updateAdvisorStmt.setInt(2, accountId);

                    // Execute the update
                    int rowsAffected = updateAdvisorStmt.executeUpdate();

                    // Check if the update was successful
                    if (rowsAffected > 0) {
                        return true;
                    } else {
                        return false;
                    }
                }
            } catch (SQLException e) {
                System.out.println("SQL error occurred while changing advisors: " + e.getMessage());
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return false; // Return false if the method execution fails at any point
    }

    /**
     * Checks if an advisor with the specified ID exists in the database.
     *
     * @param connection the database connection
     * @param advisorId  the ID of the advisor to check
     * @return true if the advisor exists, false otherwise
     * @throws SQLException if a database error occurs
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
}
