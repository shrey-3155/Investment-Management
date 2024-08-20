package Validations;

public class AccountValidation {
    public AccountValidation(){

    }

    public boolean validateAddAccount(int clientId, int financialAdvisor, String accountName, String profileType){
        if(clientId<=0 || financialAdvisor<=0){
            return false;
        }
        if(accountName==null||accountName.isEmpty() || profileType==null||profileType.isEmpty()){
            return false;
        }
        return true;
    }

    public boolean validateTrade( int account, String stockSymbol, int sharesExchanged){
        if(account<=0 || stockSymbol==null || sharesExchanged==0){
            return false;
        }
        return true;
    }
}
