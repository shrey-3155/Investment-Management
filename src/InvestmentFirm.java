import Database.CreateDatabase;
import Manager.*;
import Reporting.*;
import Validations.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InvestmentFirm {
    SectorManager sectorManager;
    StockManager stockManager;
    ProfileManager profileManager;
    AdvisorManager advisorManager;
    ClientManager clientManager;
    AccountManager accountManager;
    DivergentAccount divergentAccount ;
    SectorWeights sectorWeights;
    ReportingOfSystem reportingOfSystem ;
    DividendManager dividendManager;
    RecommendationSystem recommendationSystem ;
    AccountValidation accountValidation ;
    StockValidation stockValidation;
    SectorValidation sectorValidation;
    ReportingValidation reportingValidation ;
    ProfileValidation profileValidation ;
    AdvisorClientValidation advisorClientValidation ;
    AdvisorGroups advisorGroups ;
    AnalysisValidation analysisValidation;

    public InvestmentFirm() {
        CreateDatabase.createDatabase();
        sectorManager = new SectorManager();
        stockManager = new StockManager();
        profileManager = new ProfileManager();
        advisorManager = new AdvisorManager();
        clientManager = new ClientManager();
        accountManager = new AccountManager();
        divergentAccount = new DivergentAccount();
        sectorWeights = new SectorWeights();
        reportingOfSystem = new ReportingOfSystem();
        dividendManager = new DividendManager();
        recommendationSystem = new RecommendationSystem();
        accountValidation = new AccountValidation();
        stockValidation = new StockValidation();
        sectorValidation = new SectorValidation();
        reportingValidation = new ReportingValidation();
        profileValidation = new ProfileValidation();
        advisorClientValidation = new AdvisorClientValidation();
        advisorGroups = new AdvisorGroups();
        analysisValidation = new AnalysisValidation();
    }

    // Method to declare a sector
    public boolean defineSector(String sectorName) {
        if (sectorValidation.validateSector(sectorName)) {
            return sectorManager.addSector(sectorName);
        }
        return false;
    }

    // Method to declare a stock
    public boolean defineStock(String companyName, String stockSymbol, String sector) {
        if (stockValidation.validateStocks(companyName, stockSymbol, sector)) {
            return stockManager.addStock(companyName, stockSymbol, sector);
        }
        return false;
    }

    // Method to set the stock price
    public boolean setStockPrice(String stockSymbol, double perSharePrice) throws SQLException {
        if (stockValidation.validateStockPrice(stockSymbol, perSharePrice)) {
            return stockManager.setStockPrice(stockSymbol, perSharePrice);
        }
        return false;
    }

    // Method to define a profile
    public boolean defineProfile(String profileName, Map<String, Integer> sectorHoldings) {
        if (profileValidation.validateProfile(profileName, sectorHoldings)) {
            return profileManager.defineProfile(profileName, sectorHoldings);
        }
        return true;
    }

    // Method to add a financial advisor
    public int addAdvisor(String advisorName) {
        if (advisorClientValidation.validateAdvisor(advisorName)) {
            return advisorManager.addAdvisor(advisorName);
        }
        return 0;
    }

    // Method to add a client
    public int addClient(String clientName) {
        if (advisorClientValidation.validateClient(clientName)) {
            return clientManager.addClient(clientName);
        }
        return 0;
    }

    // Method to create an account
    public int createAccount(int clientId, int financialAdvisor, String accountName, String profileType, boolean reinvest) throws SQLException {
        if (accountValidation.validateAddAccount(clientId, financialAdvisor, accountName, profileType)) {
            return accountManager.createAccount(clientId, financialAdvisor, accountName, profileType, reinvest); // placeholder return value
        }
        return 0;
    }

    // Method to trade shares
    public boolean tradeShares(int account, String stockSymbol, int sharesExchanged) {
        if (accountValidation.validateTrade(account, stockSymbol, sharesExchanged)) {
            return accountManager.tradeShares(account, stockSymbol, sharesExchanged);
        }
        return false;
    }

    // Method to change the advisor for an account
    public boolean changeAdvisor(int accountId, int newAdvisorId) {
        if (advisorClientValidation.changeValidate(accountId, newAdvisorId)) {
            return advisorManager.changeAdvisor(accountId, newAdvisorId);
        }
        return false;
    }

    public double accountValue(int accountId) {
        if (reportingValidation.accountValuevalidation(accountId)) {
            return reportingOfSystem.accountValue(accountId);
        }
        return 0;
    }

    // Method to get the portfolio value for an advisor
    public double advisorPortfolioValue(int advisorId) {
        if (reportingValidation.advisorPortfolioValuevalidation(advisorId)) {
            return reportingOfSystem.advisorPortfolioValue(advisorId);
        }
        return 0;
    }

    // Method to get the profit for an investor
    public Map<Integer, Double> investorProfit(int clientId) {
        Map<Integer,Double> profit = new HashMap<>();
        if (reportingValidation.clientProfit(clientId)) {
            return reportingOfSystem.investorProfit(clientId);
        }
        return profit;
    }

    // Method to get the profile sector weights
    public Map<String, Integer> profileSectorWeights(int accountId) {
        Map<String,Integer> weights = new HashMap<>();
        if (reportingValidation.sectorWeightsValidation(accountId)) {
            return sectorWeights.profileSectorWeights(accountId);
        }
        return weights;
    }

    // Method to find divergent accounts
    public Set<Integer> divergentAccounts(int tolerance) {
        Set<Integer> divergence = new HashSet<>();
        if (reportingValidation.divergentAccountsValidation(tolerance)) {
            return divergentAccount.divergentAccounts(tolerance);
        }
        return divergence;
    }

    // Method to disburse dividends
    public int disburseDividend(String stockSymbol, double dividendPerShare) {
        if (reportingValidation.dividendValidation(stockSymbol, dividendPerShare)) {
            return dividendManager.disburseDividend(stockSymbol, dividendPerShare);
        }
        return 0;
    }

    // Method to get stock recommendations
    public Map<String, Boolean> stockRecommendations(int accountId, int maxRecommendations, int numComparators) {
        Map<String,Boolean> recommendations = new HashMap<>();
        if(analysisValidation.stockRecValidations(accountId,maxRecommendations,numComparators)){
            return recommendationSystem.stockRecommendations(accountId, maxRecommendations, numComparators);

        }
        return recommendations;
    }

    // Method to find advisor groups
    public Set<Set<Integer>> advisorGroups(double tolerance, int maxGroups) {
        Set<Set<Integer>> groups = new HashSet<>();
        if(analysisValidation.advisorgroupValidations(tolerance,maxGroups)){
            return advisorGroups.advisorGroups(tolerance,maxGroups);
        }
        return groups;
    }


}
