package Reporting;

import Database.ConnectionEstablisher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationSystem {
    ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher();

    public RecommendationSystem() {

    }

    /**
     * Generates stock recommendations for a given account.
     *
     * @param accountId          the ID of the account
     * @param maxRecommendations the maximum number of recommendations to generate
     * @param numComparators     the number of comparator accounts to consider
     * @return a map of stock symbols to buy/sell flags
     * References for concept for the below method: https://en.wikipedia.org/wiki/Cosine_similarity
     */
    public Map<String, Boolean> stockRecommendations(int accountId, int maxRecommendations, int numComparators) {
        Connection connection = connectionEstablisher.establishConnection();
        Map<String, Boolean> recommendations = new HashMap<>();
        if (connection != null) {
            try {
                Map<Integer, Double> stocksForGivenAccount = getStocksForGivenAccount(accountId);
                Map<Integer, Map<Integer, Double>> accountHoldings = getStockHoldingsForAccount(accountId);
                if(numComparators>accountHoldings.size()){
                    return recommendations;
                }

                Map<Integer, Double> comparatorAccounts = getComparatorAccounts(accountId, stocksForGivenAccount, accountHoldings);
                List<Map.Entry<Integer, Double>> sortedAccounts = new ArrayList<>(comparatorAccounts.entrySet());
                sortedAccounts.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
                Map<Integer, Double> topComparators = new LinkedHashMap<>();
                int count = 0;
                for (Map.Entry<Integer, Double> entry : sortedAccounts) {
                    if (count < numComparators) {
                        topComparators.put(entry.getKey(), entry.getValue());
                    }
                    count++;
                }

                Map<String, Integer> stocksToBuy = new HashMap<>();
                Map<String, Integer> stocksToSell = new HashMap<>();
                Map<Boolean, Map<String, Integer>> combinedMap = new HashMap<>();
                for (Map.Entry<Integer, Map<Integer, Double>> entry : accountHoldings.entrySet()) {
                    int accId = entry.getKey();
                    Map<Integer, Double> holdings = entry.getValue();
                    for (Map.Entry<Integer, Double> stockEntry : holdings.entrySet()) {

                        int stockId = stockEntry.getKey();
                        double quantity = stockEntry.getValue();
                        if (stocksForGivenAccount.containsKey(stockId)) {
                            // Account doesn't have this stock
                            int majorityHasStock = majorityHasStock(stockId, topComparators, accountHoldings, stocksForGivenAccount);
                            int majorityDoesNoHasStock = majorityDoesNotHasStock(stockId, topComparators, accountHoldings, stocksForGivenAccount);
                            if (majorityHasStock > 0) {
                                String name = getStockSymbolByStockId(stockId);
                                stocksToBuy.put(name, majorityHasStock);
                            }
                            if (majorityDoesNoHasStock > 0) {
                                String name = getStockSymbolByStockId(stockId);
                                stocksToSell.put(name, majorityDoesNoHasStock);
                            }
                        }
                    }
                }
                combinedMap.put(true, stocksToBuy);
                combinedMap.put(false, stocksToSell);

                // Define a custom comparator
                Comparator<Map.Entry<String, Integer>> entryComparator = Map.Entry.comparingByValue(Comparator.reverseOrder());

                combinedMap.forEach((key, value) -> {
                    Map<String, Integer> sortedInnerMap = value.entrySet().stream()
                            .sorted(entryComparator)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
                    combinedMap.put(key, sortedInnerMap);
                });
                Map<Boolean, Map<String, Integer>> sortedCombinedMap = combinedMap.entrySet().stream()
                        .sorted((e1, e2) -> {
                            int sum1 = e1.getValue().values().stream().mapToInt(Integer::intValue).sum();
                            int sum2 = e2.getValue().values().stream().mapToInt(Integer::intValue).sum();
                            return Integer.compare(sum1, sum2);
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));


                for (Map.Entry<Boolean, Map<String, Integer>> entry : sortedCombinedMap.entrySet()) {
                    boolean toBuy = entry.getKey();
                    Map<String, Integer> stocks = entry.getValue();

                    // Iterate through the stocks
                    for (Map.Entry<String, Integer> stockEntry : stocks.entrySet()) {
                        String stockSymbol = stockEntry.getKey();

                        // Check if adding the current entry would exceed maxRecommendations
                        if (recommendations.size() < maxRecommendations) {
                            // Add the stock to recommendations
                            recommendations.put(stockSymbol, toBuy);
                        } else {
                            // Break out of the loop if the maxRecommendations limit is reached
                            break;
                        }
                    }
                    // Break out of the loop if the maxRecommendations limit is reached
                    if (recommendations.size() >= maxRecommendations) {
                        break;
                    }
                }


            } finally {
                connectionEstablisher.closeConnection(connection);
            }

        } else {
            System.out.println("Failed to establish database connection.");
        }
        return recommendations;
    }

    /**
     * Retrieves the stock symbol corresponding to a given stock ID.
     *
     * @param stockId the ID of the stock
     * @return the stock symbol
     */
    private String getStockSymbolByStockId(int stockId) {
        Connection connection = connectionEstablisher.establishConnection();
        String stockSymbol = null;

        if (connection != null) {
            try {
                String query = "SELECT stockSymbol FROM Stocks WHERE stock_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, stockId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        stockSymbol = rs.getString("stockSymbol");
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

        return stockSymbol;
    }


    /**
     * Determines the majority of comparator accounts that have a particular stock.
     *
     * @param stockId               the ID of the stock
     * @param topComparators        the top comparator accounts
     * @param accountHoldings       the holdings of all accounts
     * @param stocksForGivenAccount the stocks held by the given account
     * @return the count of accounts in majority that have the stock
     */
    private int majorityHasStock(int stockId, Map<Integer, Double> topComparators, Map<Integer, Map<Integer, Double>> accountHoldings, Map<Integer, Double> stocksForGivenAccount) {
        int countWithStock = 0;
        int totalCount = topComparators.size();
        for (Integer s : accountHoldings.keySet()) {
            if (topComparators.containsKey(s)) {
                if (stocksForGivenAccount.get(stockId) == 0 && accountHoldings.get(s).get(stockId) > 0) {
                    countWithStock++;
                }
            }
        }
        if (countWithStock > (totalCount / 2)) {
            return countWithStock;
        }
        return 0;
    }

    /**
     * Determines the majority of comparator accounts that do not have a particular stock.
     *
     * @param stockId               the ID of the stock
     * @param topComparators        the top comparator accounts
     * @param accountHoldings       the holdings of all accounts
     * @param stocksForGivenAccount the stocks held by the given account
     * @return the count of accounts in majority that do not have the stock
     */
    private int majorityDoesNotHasStock(int stockId, Map<Integer, Double> topComparators, Map<Integer, Map<Integer, Double>> accountHoldings, Map<Integer, Double> stocksForGivenAccount) {
        int countWithStock = 0;
        int totalCount = topComparators.size();
        for (Integer s : accountHoldings.keySet()) {
            if (topComparators.containsKey(s)) {
                if (stocksForGivenAccount.get(stockId) > 0 && accountHoldings.get(s).get(stockId) == 0) {
                    countWithStock++;
                }
            }
        }

        if (countWithStock > (totalCount / 2)) {
            return countWithStock;
        }
        return 0;
    }

    /**
     * Retrieves the stocks held by the given account.
     *
     * @param accountId the ID of the account
     * @return a map of stock IDs to quantities
     */
    private Map<Integer, Double> getStocksForGivenAccount(int accountId) {
        Map<Integer, Double> stocksForAccount = new HashMap<>();

        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                Set<Integer> allStockIds = new HashSet<>();
                String stockIdsQuery = "SELECT stock_id FROM Stocks";
                try (PreparedStatement stockIdsStmt = connection.prepareStatement(stockIdsQuery);
                     ResultSet stockIdsResult = stockIdsStmt.executeQuery()) {
                    while (stockIdsResult.next()) {
                        allStockIds.add(stockIdsResult.getInt("stock_id"));
                    }
                }
                // Fetch stock holdings for the given account
                String query = "SELECT stock_id, SUM(quantity) AS totalQuantity " +
                        "FROM Investments WHERE account_id = ? GROUP BY stock_id";

                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, accountId);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        int stockId = rs.getInt("stock_id");
                        double totalQuantity = rs.getDouble("totalQuantity");
                        stocksForAccount.put(stockId, totalQuantity);
                    }

                    for (Integer stockId : allStockIds) {
                        stocksForAccount.putIfAbsent(stockId, 0.0);
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

        return stocksForAccount;
    }

    /**
     * Retrieves the holdings of all accounts.
     *
     * @param accountId the ID of the account
     * @return a map of account IDs to their stock holdings
     */
    private Map<Integer, Map<Integer, Double>> getStockHoldingsForAccount(int accountId) {
        Map<Integer, Map<Integer, Double>> accountHoldings = new HashMap<>();

        Connection connection = connectionEstablisher.establishConnection();
        if (connection != null) {
            try {
                Set<Integer> allStockIds = new HashSet<>();
                String stockIdsQuery = "SELECT stock_id FROM Stocks";
                try (PreparedStatement stockIdsStmt = connection.prepareStatement(stockIdsQuery);
                     ResultSet stockIdsResult = stockIdsStmt.executeQuery()) {
                    while (stockIdsResult.next()) {
                        allStockIds.add(stockIdsResult.getInt("stock_id"));
                    }
                }
                // Fetch stock holdings for all accounts except the input account
                String query = "SELECT i.account_id, i.stock_id, SUM(i.quantity) AS totalQuantity " +
                        "FROM Investments i " +
                        "WHERE i.account_id != ? " +
                        "GROUP BY i.account_id, i.stock_id";

                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, accountId);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        int accId = rs.getInt("account_id");
                        int stockId = rs.getInt("stock_id");
                        Double totalQuantity = rs.getDouble("totalQuantity");

                        // Create inner map if not present
                        accountHoldings.putIfAbsent(accId, new HashMap<>());
                        // Add stock holdings for each account
                        accountHoldings.get(accId).put(stockId, totalQuantity);
                    }
                    for (Integer s : accountHoldings.keySet()) {
                        for (Integer stockId : allStockIds) {
                            accountHoldings.get(s).putIfAbsent(stockId, 0.0);
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

        return accountHoldings;
    }


    /**
     * Retrieves the comparator accounts based on cosine similarity.
     *
     * @param accountId             the ID of the account
     * @param stocksForGivenAccount the stocks held by the given account
     * @param accountHoldings       the holdings of all accounts
     * @return a map of comparator account IDs to cosine similarity scores
     */
    private Map<Integer, Double> getComparatorAccounts(int accountId, Map<Integer, Double> stocksForGivenAccount, Map<Integer, Map<Integer, Double>> accountHoldings) {
        Map<Integer, Double> comparatorAccounts = new HashMap<>();

        // Calculate the magnitude of stocksForGivenAccount
        double magnitudeStocksForGivenAccount = calculateMagnitude(stocksForGivenAccount);

        // Iterate over each account's holdings to calculate the cosine similarity
        for (Map.Entry<Integer, Map<Integer, Double>> entry : accountHoldings.entrySet()) {
            int accId = entry.getKey();
            Map<Integer, Double> holdings = entry.getValue();

            // Calculate the dot product of the two vectors
            double dotProduct = calculateDotProduct(stocksForGivenAccount, holdings);

            // Calculate the magnitude of the current account's holdings
            double magnitudeHoldings = calculateMagnitude(holdings);

            // Calculate the cosine similarity
            double cosineSimilarity = dotProduct / (magnitudeStocksForGivenAccount * magnitudeHoldings);

            // Store the cosine similarity for the current account
            comparatorAccounts.put(accId, cosineSimilarity);
        }

        return comparatorAccounts;
    }

    /**
     * Calculates the dot product of two vectors.
     *
     * @param vector1 the first vector
     * @param vector2 the second vector
     * @return the dot product
     */
    private double calculateDotProduct(Map<Integer, Double> vector1, Map<Integer, Double> vector2) {
        double dotProduct = 0.0;
        for (Map.Entry<Integer, Double> entry : vector1.entrySet()) {
            int stockId = entry.getKey();
            double quantity1 = entry.getValue();
            double quantity2 = vector2.getOrDefault(stockId, 0.0);
            dotProduct += quantity1 * quantity2;
        }
        return dotProduct;
    }

    /**
     * Calculates the magnitude of a vector.
     *
     * @param vector the vector
     * @return the magnitude
     */
    private double calculateMagnitude(Map<Integer, Double> vector) {
        double magnitude = 0.0;
        for (double quantity : vector.values()) {
            magnitude += Math.pow(quantity, 2);
        }
        return Math.sqrt(magnitude);
    }


}
