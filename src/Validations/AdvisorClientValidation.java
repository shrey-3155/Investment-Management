package Validations;

public class AdvisorClientValidation {
    public AdvisorClientValidation(){
    }

    public boolean validateAdvisor(String advisorName){
        if(advisorName==null || advisorName.isEmpty()){
            return false;
        }
        return true;
    }

    public boolean validateClient(String clientName){
        if(clientName==null || clientName.isEmpty()){
            return false;
        }
        return true;
    }

    public boolean changeValidate( int accountId, int newAdvisorId){
        if(accountId<=0 || newAdvisorId<=0){
            return false;
        }
        return true;
    }
}
