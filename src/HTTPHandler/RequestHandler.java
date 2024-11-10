package HTTPHandler;

import structure.*;
import java.net.*;
import java.util.ArrayList;

public interface RequestHandler {
    AdditionalInfo info = new AdditionalInfo();
    default void setAdditionalInfo(ArrayList<StructureDest> dest, boolean hasCookie, String det, String UserID) {
        info.setDest(dest);
        info.setHasCookie(hasCookie);
        info.setUserID(UserID);
        info.setDet(det);
    }

    void handle(Socket client) throws Exception;
    default String makeRandomID() {
        int[] ids = new int[10];
        String ID = "";
        for (int i = 0; i < 10; i++) {
            ids[i] = (int)(Math.random() * 10);
            ID += Integer.toString(ids[i]);
        }

        return ID;
    }
}
