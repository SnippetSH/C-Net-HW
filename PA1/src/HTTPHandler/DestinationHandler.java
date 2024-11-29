package HTTPHandler;

import helper.JsonFileHandler;
import structure.AdditionalInfo;
import structure.StructureDest;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DestinationHandler implements RequestHandler {
    boolean cookieEnabled = true;
    public DestinationHandler(boolean cookieEnabled) {
        this.cookieEnabled = cookieEnabled;
    }
    @Override
    public void handle(Socket client) throws Exception {
        try (
            OutputStream out = client.getOutputStream()
        ) {
            ArrayList<StructureDest> destinations = info.getDest();
            String dest = info.getDet();
            boolean hasCookie = info.getHasCookie();
            String UserID = info.getUserID();

            ArrayList<String> names = new ArrayList<>();
            for (StructureDest dst : destinations) {
                if (dst.getType().equals(dest.replace("/", ""))) {
                    names.add(dst.getName());
                }
            }

            if (!hasCookie && cookieEnabled) {
                UserID = makeRandomID();
                System.out.println("New user requested page, cookie will be set.");
            }

            JsonFileHandler.writeJson("data.json", UserID, dest);

            String filePath = "static/destination.html";
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            String content = new String(fileContent, StandardCharsets.UTF_8);

            if (names.size() < 2) {
                System.out.println(names.size());
                throw new ArrayIndexOutOfBoundsException("Parsed Data is invalid.\n");
            }
            content = content.replace("TRAVEL DESTINATION TITLE 1", names.get(0));
            content = content.replace("TRAVEL DESTINATION TITLE 2", names.get(1));
            content = content.replace("/destination1", names.get(0).replace(" ", ""));
            content = content.replace("/destination2", names.get(1).replace(" ", ""));

            fileContent = content.getBytes();

            String header = "";
            if (cookieEnabled) {
                header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + fileContent.length + "\r\n" +
                        "Set-Cookie: UserID=" + UserID + "; Path=/; Max-Age=3600\r\n" +
                        "\r\n";
            } else {
                header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + fileContent.length + "\r\n" +
                        "\r\n";
            }

            out.write(header.getBytes());
            out.write(fileContent);
            out.flush();

            System.out.println("destination.html was served.");
        }
    }

}
