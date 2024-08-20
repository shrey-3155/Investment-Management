package Validations;

public class StockValidation {

    public StockValidation(){

    }

    public boolean validateStocks(String companyName, String stockSymbol, String sector){
        if(companyName==null||companyName.isEmpty()||stockSymbol==null||stockSymbol.isEmpty()||sector==null||sector.isEmpty()){
            return false;
        }
        return true;
    }

    public boolean validateStockPrice( String stockSymbol, double perSharePrice){
        if(stockSymbol==null || stockSymbol.isEmpty() || perSharePrice<=0){
            return false;
        }
        return true;
    }

}
