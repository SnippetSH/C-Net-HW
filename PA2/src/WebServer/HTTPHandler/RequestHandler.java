package WebServer.HTTPHandler;

import WebServer.structure.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

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

    default String formatDataToHTTP(long timeStamp) {
        SimpleDateFormat httpDataFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        httpDataFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return httpDataFormat.format(new Date(timeStamp));
    }
}
