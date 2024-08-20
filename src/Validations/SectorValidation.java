package Validations;

public class SectorValidation {
    public SectorValidation(){

    }

    public boolean validateSector( String sectorName){
        if(sectorName==null || sectorName.isEmpty()){
            return false;
        }
        return true;
    }

}
