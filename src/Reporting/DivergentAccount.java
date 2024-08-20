package Reporting;

import Database.ConnectionEstablisher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DivergentAccount {

    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();
    SectorWeights sectorWeights = new SectorWeights();

    public DivergentAccount() {

    }

    /**
     * Identifies divergent accounts based on sector distributions.
     *
     * @param tolerance the allowed tolerance for divergence
     * @return a set of divergent account IDs
     */
    public Set<Integer> divergentAccounts(int tolerance) {
        Set<Integer> divergentAccountIds = new HashSet<>();
        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                // Fetch all accounts
                String fetchAccountsSQL = "SELECT account_id FROM Accounts";
                try (PreparedStatement accountsStmt = connection.prepareStatement(fetchAccountsSQL)) {
                    ResultSet accountsRS = accountsStmt.executeQuery();
                    while (accountsRS.next()) {
                        int accountId = accountsRS.getInt("account_id");
                        // For each account, calculate the current sector distributions
                        Map<String, Integer> currentDistributions = sectorWeights.profileSectorWeights(accountId);
                        // Fetch the target profile sector distributions for this account
                        Map<String, Integer> targetDistributions = fetchTargetDistributions(accountId, connection);
                        // Check if the account's current distributions diverge from the target by more than the tolerance
                        if (isDivergent(currentDistributions, targetDistributions, tolerance)) {
                            divergentAccountIds.add(accountId);
                        }
                    }
                }
            } catch (SQLException e) {
                System.out.println("SQL error occurred: " + e.getMessage());
            } finally {
                connectionEstablisher.closeConnection(connection);
            }
        } else {
            System.out.println("Failed to establish database connection.");
        }
        return divergentAccountIds;
    }

    /**
     * Fetches the target sector distributions for an account.
     *
     * @param accountId  the ID of the account
     * @param connection the database connection
     * @return a map of sector names to percentages
     * @throws SQLException if a SQL error occurs
     */
    private Map<String, Integer> fetchTargetDistributions(int accountId, Connection connection) throws SQLException {
        Map<String, Integer> targetDistributions = new HashMap<>();
        String sql = "SELECT sec.sectorName, psh.percentage " +
                "FROM Accounts acc " +
                "JOIN Profiles prof ON acc.profile_id = prof.profile_id " +
                "JOIN Profile_Sector_Holdings psh ON prof.profile_id = psh.profile_id " +
                "JOIN Sectors sec ON psh.sector_id = sec.sector_id " +
                "WHERE acc.account_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String sectorName = rs.getString("sectorName");
                    int percentage = rs.getInt("percentage");
                    targetDistributions.put(sectorName, percentage);
                }
            }
        }
        return targetDistributions;
    }

    /**
     * Checks if the sector distributions are divergent.
     *
     * @param current   the current sector distributions
     * @param target    the target sector distributions
     * @param tolerance the allowed tolerance for divergence
     * @return true if the sector distributions are divergent, false otherwise
     */
    private boolean isDivergent(Map<String, Integer> current, Map<String, Integer> target, int tolerance) {
        for (Map.Entry<String, Integer> targetEntry : target.entrySet()) {
            String sector = targetEntry.getKey();
            int targetPercentage = targetEntry.getValue();
            int currentPercentage = current.getOrDefault(sector, 0); // Use 0 if the sector is not present in current distributions

            // Calculate the divergence
            if (Math.abs(currentPercentage - targetPercentage) > tolerance) {
                return true; // Found a sector that diverges more than the allowed tolerance
            }
        }
        // Check for sectors present in current but not in target distributions
        for (String currentSector : current.keySet()) {
            if (!target.containsKey(currentSector) && current.get(currentSector) > tolerance) {
                return true; // Found an unexpected sector with significant holding
            }
        }
        return false; // No significant divergence found
    }
}
