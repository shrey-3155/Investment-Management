package Validations;

public class ReportingValidation {

    public ReportingValidation(){

    }

    public boolean accountValuevalidation(int accountId){
        if(accountId<=0){
            return false;
        }
        return true;
    }


    public boolean advisorPortfolioValuevalidation( int advisorId){
        if(advisorId<=0){
            return false;
        }
        return true;
    }

    public boolean clientProfit(int Clientid){
        if(Clientid<=0){
            return false;
        }
        return true;
    }

    public boolean sectorWeightsValidation( int accountId){
        if(accountId<=0){
            return false;
        }
        return true;
    }

    public boolean divergentAccountsValidation( int tolerance ){
        if(tolerance<=0 || tolerance>100){
            return false;
        }
        return true;
    }

    public boolean dividendValidation(String stockSymbol, double dividendPerShare){
        if(stockSymbol==null || stockSymbol.isEmpty() || dividendPerShare<=0){
            return false;
        }
        return true;
    }
}
