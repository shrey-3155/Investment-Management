package Validations;

public class AnalysisValidation {
    public AnalysisValidation(){

    }

    public boolean stockRecValidations(int accountId, int maxRecommendations, int
            numComparators){
        if(accountId<=0 || maxRecommendations<=0 || numComparators<=0){
            return false;
        }
        return true;
    }

    public boolean advisorgroupValidations(double tolearance, int maxGroups){
        if(tolearance <-1 || tolearance>1 || maxGroups<=0){
            return false;
        }
        return true;
    }
}
