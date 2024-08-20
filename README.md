# Investment-Management

**Investment-Management** is a core Java application designed to address the complexities and inefficiencies of managing client portfolios within an investment firm, particularly those focused on stock investments. This project provides a comprehensive solution for tracking investments, managing portfolios, and automating key processes like dividend reinvestment.

## Project Overview

The application tackles challenges faced by investment management firms, such as accurately tracking fractional shares, managing large numbers of investors, and ensuring precise dividend reinvestment. By implementing the `InvestmentFirm` class and associated modules, the system offers a robust framework for managing investments, validating data, and integrating with backend databases.

## Key Features

- **Portfolio Management**: Define sectors, stocks, and profiles to create and manage individual investment portfolios.
- **Advisor and Client Management**: Add financial advisors and clients, and manage their interactions within the system.
- **Trading Functionality**: Perform buy/sell operations on stocks, and manage client accounts with ease.
- **Dividend Management**: Disburse dividends automatically to investors.
- **Reporting and Analysis**: Generate reports on account values, advisor portfolio values, and investor profits. Analyze investment patterns and provide stock recommendations.

## System Modules

- **Database Module**: Manages connections to the database, ensuring efficient data retrieval and storage.
- **Manager Module**: Contains multiple classes to handle sectors, stocks, accounts, profiles, and more. These classes interact with the `InvestmentFirm` class to manage the data.
- **Validation Module**: Ensures data integrity and validation, connecting directly to the backend database.

## Core Methods

- **Getting Data into the System**:
  - `defineSector(String sectorName)`: Set up new investment sectors.
  - `defineStock(String companyName, String stockSymbol, String sector)`: Define and manage stocks.
  - `defineProfile(String profileName, Map<String, Integer> sectorHoldings)`: Create investment profiles categorized by sector.
  - `addAdvisor(String advisorName)`: Add financial advisors to the system.
  - `addClient(String clientName)`: Register clients within the system.
  - `createAccount(int clientId, int financialAdvisor, String accountName, String profileType, boolean reinvest)`: Open new investment accounts for clients.

- **Trading Operations**:
  - `tradeShares(int account, String stockSymbol, int sharesExchanged)`: Execute trades within client accounts.
  - `changeAdvisor(int accountId, int newAdvisorId)`: Change the financial advisor associated with a client account.

- **Reporting and Analysis**:
  - `accountValue(int accountId)`: Calculate the market value of an investment account.
  - `advisorPortfolioValue(int advisorId)`: Assess the average value of portfolios managed by an advisor.
  - `investorProfit(int clientId)`: Compute potential profits from selling all stocks in a client's accounts.
  - `profileSectorWeights(int accountId)`: Analyze sector exposure within a portfolio.
  - `stockRecommendations(int accountId, int maxRecommendations, int numComparators)`: Provide stock recommendations based on account profiles and market conditions.

## Database Schema

The project employs a carefully structured database schema to store and manage investment data. This schema supports the tracking of stocks, sectors, client accounts, and advisor portfolios, ensuring efficient and accurate data management.
