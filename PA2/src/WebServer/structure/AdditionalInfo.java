package WebServer.structure;

import java.util.ArrayList;

public class AdditionalInfo {
    private String UserID;
    private boolean hasCookie;
    private String det;
    private ArrayList<StructureDest> dest;

    public void setUserID(String UserID) {
        this.UserID = UserID;
    }
    public void setHasCookie(boolean hasCookie) {
        this.hasCookie = hasCookie;
    }
    public void setDet(String det) {
        this.det = det;
    }
    public void setDest(ArrayList<StructureDest> dest) {
        this.dest = dest;
    }

    public String getUserID() { return this.UserID; }
    public String getDet() { return this.det; }
    public boolean getHasCookie() { return this.hasCookie; }
    public StructureDest getDest(int idx) {
        if (idx >= dest.toArray().length) {
            return null;
        }
        return dest.get(idx);
    }
    public ArrayList<StructureDest> getDest() {
        return this.dest;
    }
}
