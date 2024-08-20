package Manager;

import Database.ConnectionEstablisher;

import java.sql.*;

public class ClientManager {

    /**
     * The connection establisher used for connecting to the database.
     */
    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();

    public ClientManager() {

    }


    /**
     * Adds a new client with the specified name to the database.
     *
     * @param clientName the name of the client to add
     * @return the ID of the newly added client, or -1 if addition fails
     */
    public int addClient(String clientName) {
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            String insertAdvisorSQL = "INSERT INTO Clients (clientName) VALUES (?)";
            try (PreparedStatement insertAdvisorStmt = connection.prepareStatement(insertAdvisorSQL, Statement.RETURN_GENERATED_KEYS)) {
                insertAdvisorStmt.setString(1, clientName);

                int rowsAffected = insertAdvisorStmt.executeUpdate();
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = insertAdvisorStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int clientId = generatedKeys.getInt(1); // Obtain the generated advisor ID
                            return clientId;
                        } else {
                            throw new SQLException("Creating Client failed, no ID obtained.");
                        }
                    }
                } else {
                    System.out.println("Client not added: " + clientName);
                    return -1; // Indicate failure
                }
            } catch (SQLException e) {
                if (e.getSQLState().startsWith("23")) { // Unique constraint violation
                    System.out.println("Advisor already exists: " + clientName);
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

}
