package Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateDatabase {
    static ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();
   public static String schema =
           "CREATE TABLE IF NOT EXISTS Sectors (" +
                   "    sector_id INT AUTO_INCREMENT PRIMARY KEY," +
                   "    sectorName VARCHAR(100) UNIQUE" +
                   ");\n\n" +
            "CREATE TABLE IF NOT EXISTS Advisors (" +
                    "    advisor_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    advisorName VARCHAR(100) UNIQUE" +
                    ");\n\n" +

                    "CREATE TABLE IF NOT EXISTS Clients (" +
                    "    client_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    clientName VARCHAR(100) UNIQUE" +
                    ");\n\n" +

                    "CREATE TABLE IF NOT EXISTS Profiles (" +
                    "    profile_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    profileName VARCHAR(100) UNIQUE" +
                    ");\n\n" +

                    "CREATE TABLE IF NOT EXISTS Stocks (" +
                    "    stock_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    stockName VARCHAR(100)," +
                    "    stockSymbol VARCHAR(20) UNIQUE," +
                    "    sector_id INT," +
                    "    perShare_price DOUBLE default 1," +
                    "    FOREIGN KEY (sector_id) REFERENCES Sectors(sector_id)" +
                    ");\n\n" +

                    "CREATE TABLE IF NOT EXISTS Accounts (" +
                    "    account_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    client_id INT," +
                    "    advisor_id INT," +
                    "    accountName VARCHAR(100)," +
                    "    profile_id INT," +
                    "    reinvest boolean," +
                    "    cash_balance DECIMAL(18,2) default 0," +
                   "    UNIQUE(client_id, accountName)," +
                   "    FOREIGN KEY (client_id) REFERENCES Clients(client_id)," +
                    "    FOREIGN KEY (advisor_id) REFERENCES Advisors(advisor_id)," +
                    "    FOREIGN KEY (profile_id) REFERENCES Profiles(profile_id)" +
                    ");\n\n" +

                    "CREATE TABLE IF NOT EXISTS Profile_Sector_Holdings (" +
                    "    profile_id INT," +
                    "    sector_id INT," +
                    "    percentage INT," +
                    "    PRIMARY KEY (profile_id, sector_id)," +
                    "    FOREIGN KEY (profile_id) REFERENCES Profiles(profile_id)," +
                    "    FOREIGN KEY (sector_id) REFERENCES Sectors(sector_id)" +
                    ");\n\n" +

                    "CREATE TABLE IF NOT EXISTS Investments (" +
                    "    investment_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    account_id INT NOT NULL," +
                    "    stock_id INT NOT NULL," +
                    "    quantity DECIMAL(18, 2) NOT NULL," +
                    "    acb DECIMAL(18,2) NOT NULL DEFAULT 0," +
                    "    FOREIGN KEY (account_id) REFERENCES Accounts(account_id)," +
                    "    FOREIGN KEY (stock_id) REFERENCES Stocks(stock_id)," +
                    "    UNIQUE KEY account_stock_unique (account_id, stock_id)" +
                    ");\n\n" +

                    "CREATE TABLE IF NOT EXISTS FirmStockHoldings (" +
                    "    firm_holding_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    stock_id INT NOT NULL," +
                    "    stocks_owned DECIMAL(10, 4) NOT NULL DEFAULT 0," +
                    "    FOREIGN KEY (stock_id) REFERENCES Stocks(stock_id)," +
                    "    UNIQUE (stock_id)" +
                    ");\n\n" +

                    "ALTER TABLE Investments\n" +
                    "MODIFY COLUMN quantity DECIMAL(18, 2) NOT NULL;";


    public CreateDatabase(){

    }

    public static boolean createDatabase() {
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String[] createTableQueries = schema.split(";\n\n");
                for (String query : createTableQueries) {
                    statement.executeUpdate(query);
                }
                return true; // Database creation successful
            } catch (SQLException e) {
                return false; // Database creation failed
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            return false; // Database connection failed
        }
    }

}
