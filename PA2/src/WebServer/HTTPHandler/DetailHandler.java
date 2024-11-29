package WebServer.HTTPHandler;

import WebServer.helper.JsonFileHandler;
import WebServer.structure.AdditionalInfo;
import WebServer.structure.StructureDest;

import java.io.File;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DetailHandler implements RequestHandler {
    boolean cookieEnabled = true;
    public DetailHandler(boolean cookieEnabled) {
        this.cookieEnabled = cookieEnabled;
    }

    @Override
    public void handle(Socket client) throws Exception {
        try (
            OutputStream out = client.getOutputStream();
        ) {
            ArrayList<StructureDest> destinations = info.getDest();
            String detail = info.getDet();
            boolean hasCookie = info.getHasCookie();
            String UserID = info.getUserID();

            StructureDest used = null;
            for (StructureDest dst : destinations) {
                if ((detail.replace("/", "")).equals(dst.getName().replace(" ", ""))) {
                    used = new StructureDest(dst);
                }
            }

            if (!hasCookie && cookieEnabled) {
                UserID = makeRandomID();
                System.out.println("New user requested page, cookie will be set.");
            }

            if (used == null) {
                throw new NullPointerException("Parsed data is invalid");
            }

            System.out.println(used.getName());
            JsonFileHandler.writeJson("data.json", UserID, used.getType());

            String filePath = "resources/detail.html";
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            String content = new String(fileContent, StandardCharsets.UTF_8);

            content = content.replace("TRAVEL DESTINATION TITLE", used.getName());
            content = content.replace("TRAVEL DESTINATION TYPE", used.getType());
            content = content.replace("TRAVEL DESTINATION DESCRIPTION", used.getDescription());
            content = content.replace("IMAGE SRC", "resources/"+used.getImage().replace("//", "/"));

            fileContent = content.getBytes();

            String header = "";
            if (cookieEnabled) {
                header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + fileContent.length + "\r\n" +
                        "Set-Cookie: UserID=" + UserID + "; Path=/; Max-Age=3600\r\n";
            } else {
                header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + fileContent.length + "\r\n";
            }

            if (hasCookie) {
                header += "\r\n";
            } else {
                File file = new File("resources/detail.html");
                long lastModified = file.lastModified();
                String lastModifiedString = formatDataToHTTP(lastModified);
                header += "Cache-Control: max-age=60\r\n" +
                        "Last-Modified: " + lastModifiedString + "\r\n" +
                        "\r\n";
            }

            out.write(header.getBytes());
            out.write(fileContent);
            out.flush();

            System.out.println("detail.html was served.");
        }
    }
}
