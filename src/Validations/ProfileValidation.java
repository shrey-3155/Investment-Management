package Validations;

import java.util.Map;

public class ProfileValidation {
    public ProfileValidation(){

    }

    public boolean validateProfile(String profileName, Map<String, Integer> sectorHoldings){
        if(profileName==null || profileName.isEmpty() || sectorHoldings==null || sectorHoldings.isEmpty()){
            return false;
        }
        return true;
    }
}
