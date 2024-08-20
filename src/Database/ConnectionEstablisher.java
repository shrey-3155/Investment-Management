package Database;

import java.sql.*;

public class ConnectionEstablisher {

    //Static variables for database connection
    private final String URL = "jdbc:mysql://db.cs.dal.ca:3306/shrey";
    private final String USERNAME = "shrey";
    private final String PASSWORD = "B00960433";


    public ConnectionEstablisher() {
    }

    /**
     * Establishes a connection to the database using the provided credentials.
     *
     * @return a Connection object representing the database connection
     */
    public Connection establishConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            return null;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the given database connection.
     *
     * @param connect the Connection object to be closed
     */
    public void closeConnection(Connection connect) {
        if (connect != null) {
            try {
                connect.close();
            } catch (SQLException e) {
                System.out.println("Failed to close connection: " + e.getMessage());
            }
        }
    }
}
