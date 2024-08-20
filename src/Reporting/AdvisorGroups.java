package Reporting;

import Database.ConnectionEstablisher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AdvisorGroups {
    public AdvisorGroups() {

    }

    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();
    SectorWeights newsectorWeights = new SectorWeights();

    /**
     * Clusters advisor accounts based on sector difference vectors.
     *
     * @param tolerance the maximum allowed distance for convergence
     * @param maxGroups the maximum number of advisor groups to create
     * @return a set of sets, where each inner set represents an advisor group
     * Reference for the concept: https://en.wikipedia.org/wiki/K-means_clustering
     */
    public Set<Set<Integer>> advisorGroups(double tolerance, int maxGroups) {
        Map<Integer, Map<String, Integer>> sectorDifferenceVectors = extractSectorDifferenceVectors();
        Set<Set<Integer>> groups = new HashSet<>();

        // Start with 1 group and gradually increase until reaching maxGroups or convergence
        int k = 1;
        while (k <= maxGroups) {
            //random generated data points.
            List<Map<String, Integer>> clusterRepresentatives = initializeClusterRepresentatives(k);
            int converged = 0;
            Map<Integer, Integer> assignment = null;
            while (converged!=4) {
                assignment = assignToClosestCluster(sectorDifferenceVectors, clusterRepresentatives);
                converged++;
            }
            double maxDistance = calculateMaxDistance(sectorDifferenceVectors, assignment, clusterRepresentatives);
            if (maxDistance <= tolerance) {
                break;
            }
            Set<Integer> group = new HashSet<>();
            for (Map.Entry<Integer, Integer> entry : assignment.entrySet()) {
                if (entry.getValue()+1 == k) {
                    group.add(entry.getKey());
                }
            }
            groups.add(group);
            // Check if all accounts have been assigned to clusters
            if (assignment.size() == sectorDifferenceVectors.size()) {
                break; // Stop clustering if all accounts are assigned
            }
            k++; // Increment k for the next iteration
        }
        return groups;
    }

    /**
     * Extracts sector difference vectors for all advisor accounts.
     *
     * @return a map containing sector difference vectors for each advisor account
     */
    private Map<Integer, Map<String, Integer>> extractSectorDifferenceVectors() {
        Map<Integer, Map<String, Integer>> sectorDifferenceVectors = new HashMap<>();

        // Get sector weights for each account
        List<Integer> accountIds = getAllAccountIds(); // Implement this method to fetch all account IDs
        for (Integer accountId : accountIds) {
            Map<String, Integer> sectorWeights = newsectorWeights.profileSectorWeights(accountId);
            Map<String, Integer> initialWeights = getInitialSectorWeights(accountId); // Implement this method to fetch initial sector weights
            Map<String, Integer> differenceVector = calculateDifferenceVector(sectorWeights, initialWeights);
            sectorDifferenceVectors.put(accountId, differenceVector);
        }

        return sectorDifferenceVectors;
    }


    /**
     * Calculates the difference vector between sector weights and initial weights.
     *
     * @param sectorWeights  the sector weights for an account
     * @param initialWeights the initial sector weights for an account
     * @return the difference vector between sector weights and initial weights
     */
    // Method to calculate the difference between sector weights and initial weights
    private Map<String, Integer> calculateDifferenceVector(Map<String, Integer> sectorWeights, Map<String, Integer> initialWeights) {
        Map<String, Integer> differenceVector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : sectorWeights.entrySet()) {
            String sectorName = entry.getKey();
            Integer sectorWeight = entry.getValue();
            Integer initialWeight = initialWeights.getOrDefault(sectorName, 0);
            Integer difference = sectorWeight - initialWeight;
            differenceVector.put(sectorName, difference);
        }
        return differenceVector;
    }

    /**
     * Retrieves all account IDs from the database.
     *
     * @return a list of all account IDs
     */
    private List<Integer> getAllAccountIds() {
        List<Integer> accountIds = new ArrayList<>();
        Connection connection = null;
        try {
            connection = connectionEstablisher.establishConnection();
            String query = "SELECT account_id FROM Accounts";
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int accountId = resultSet.getInt("account_id");
                    accountIds.add(accountId);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            connectionEstablisher.closeConnection(connection);
        }
        return accountIds;
    }

    /**
     * Retrieves the initial sector weights for a given account.
     *
     * @param accountId the ID of the account
     * @return the initial sector weights for the account
     */
    private Map<String, Integer> getInitialSectorWeights(int accountId) {
        Map<String, Integer> initialSectorWeights = new HashMap<>();
        Connection connection = null;
        try {
            connection = connectionEstablisher.establishConnection();
            String query = "SELECT Sectors.sectorName, Profile_Sector_Holdings.percentage " +
                    "FROM Profile_Sector_Holdings " +
                    "JOIN Sectors ON Profile_Sector_Holdings.sector_id = Sectors.sector_id " +
                    "JOIN Profiles ON Profile_Sector_Holdings.profile_id = Profiles.profile_id " +
                    "JOIN Accounts ON Profiles.profile_id = Accounts.profile_id " +
                    "WHERE Accounts.account_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, accountId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String sectorName = resultSet.getString("sectorName");
                        Integer percentage = resultSet.getInt("percentage");
                        initialSectorWeights.put(sectorName, percentage);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            connectionEstablisher.closeConnection(connection);
        }
        return initialSectorWeights;
    }

    /**
     * Initializes cluster representatives with random values.
     *
     * @param k the number of clusters
     * @return a list of cluster representatives
     */
    private List<Map<String, Integer>> initializeClusterRepresentatives(int k) {
        List<Map<String, Integer>> clusterRepresentatives = new ArrayList<>();
        Random random = new Random();
        List<String> sectorNames = getAllSectorNames(); // Assuming you have a method to get all sector names
        for (int i = 0; i < k; i++) {
            Map<String, Integer> cluster = new HashMap<>();
            for (String sectorName : sectorNames) {
                int randomNumber = random.nextInt(101); // Random value in the range [0, 100]
                cluster.put(sectorName, randomNumber);
            }
            clusterRepresentatives.add(cluster);
        }
        return clusterRepresentatives;
    }

    /**
     * Retrieves all sector names from the database.
     *
     * @return a list of all sector names
     */
    private List<String> getAllSectorNames() {
        List<String> sectorNames = new ArrayList<>();
        Connection connection = null;
        try {
            connection = connectionEstablisher.establishConnection();
            String query = "SELECT sectorName FROM Sectors";
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String sectorName = resultSet.getString("sectorName");
                    sectorNames.add(sectorName);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            connectionEstablisher.closeConnection(connection);
        }
        return sectorNames;
    }


    /**
     * Assigns each account to the closest cluster based on sector difference vectors.
     *
     * @param sectorDifferenceVectors the sector difference vectors for all accounts
     * @param clusterRepresentatives  the cluster representatives
     * @return a map containing the assignment of each account to a cluster
     */
    private Map<Integer, Integer> assignToClosestCluster(Map<Integer, Map<String, Integer>> sectorDifferenceVectors, List<Map<String, Integer>> clusterRepresentatives) {
        Map<Integer, Integer> assignment = new HashMap<>();
        for (Map.Entry<Integer, Map<String, Integer>> entry : sectorDifferenceVectors.entrySet()) {
            int accountId = entry.getKey();
            Map<String, Integer> differenceVector = entry.getValue();
            int closestClusterIndex = findClosestCluster(differenceVector, clusterRepresentatives);
            assignment.put(accountId, closestClusterIndex);
        }
        return assignment;
    }

    /**
     * Finds the closest cluster to a given difference vector.
     *
     * @param differenceVector       the difference vector for an account
     * @param clusterRepresentatives the cluster representatives
     * @return the index of the closest cluster
     */
    private int findClosestCluster(Map<String, Integer> differenceVector, List<Map<String, Integer>> clusterRepresentatives) {
        int closestClusterIndex = -1;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < clusterRepresentatives.size(); i++) {
            Map<String, Integer> cluster = clusterRepresentatives.get(i);
            double distance = calculateCosineSimilarity(differenceVector, cluster);
            if (distance < minDistance) {
                minDistance = distance;
                closestClusterIndex = i;
            }
        }
        return closestClusterIndex;
    }

    /**
     * Calculates the cosine similarity between two vectors.
     *
     * @param vector1 the first vector
     * @param vector2 the second vector
     * @return the cosine similarity between the two vectors
     */
    private double calculateCosineSimilarity(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;
        for (Map.Entry<String, Integer> entry : vector1.entrySet()) {
            String sectorName = entry.getKey();
            int difference1 = entry.getValue();
            double difference2 = vector2.getOrDefault(sectorName, 0);
            dotProduct += difference1 * difference2;
            magnitude1 += Math.pow(difference1, 2);
            magnitude2 += Math.pow(difference2, 2);
        }
        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);
        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0; // Handle division by zero
        }
        return dotProduct / (magnitude1 * magnitude2);
    }

    /**
     * Calculates the maximum distance between sector difference vectors and cluster representatives.
     *
     * @param sectorDifferenceVectors the sector difference vectors for all accounts
     * @param assignment              the assignment of each account to a cluster
     * @param clusterRepresentatives  the cluster representatives
     * @return the maximum distance between sector difference vectors and cluster representatives
     */
    private double calculateMaxDistance(Map<Integer, Map<String, Integer>> sectorDifferenceVectors, Map<Integer, Integer> assignment, List<Map<String, Integer>> clusterRepresentatives) {
        double maxDistance = 0.0;
        for (Map.Entry<Integer, Map<String, Integer>> entry : sectorDifferenceVectors.entrySet()) {
            int accountId = entry.getKey();
            int clusterIndex = assignment.get(accountId);
            Map<String, Integer> differenceVector = entry.getValue();
            Map<String, Integer> clusterRepresentative = clusterRepresentatives.get(clusterIndex);
            double distance = calculateCosineSimilarity(differenceVector, clusterRepresentative);
            if (distance > maxDistance) {
                maxDistance = distance;
            }
        }
        return maxDistance;
    }

}
