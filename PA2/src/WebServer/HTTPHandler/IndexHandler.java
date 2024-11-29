package WebServer.HTTPHandler;

import WebServer.structure.AdditionalInfo;
import WebServer.helper.JsonFileHandler;

import java.io.File;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class IndexHandler implements RequestHandler{
    boolean cookieEnabled = true;
    public IndexHandler(boolean cookieEnabled) {
        this.cookieEnabled = cookieEnabled;
    }

    @Override
    public void handle(Socket client) throws Exception {
        try (
                OutputStream out = client.getOutputStream()
        ) {
            String UserID = info.getUserID();
            boolean hasCookie = info.getHasCookie();

            String filePath = "resources/index.html";
            String newUserID = makeRandomID();
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

            String header = "";
            if (cookieEnabled) {
                header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + fileContent.length + "\r\n" +
                        "Set-Cookie: UserID=" + (UserID.isEmpty() ? newUserID : UserID) + "; Path=/; Max-Age=3600\r\n";

            } else {
                header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n";
            }

            if (hasCookie) {
                String json = JsonFileHandler.readJson("data.json");
                Map<String, String> data = JsonFileHandler.parseJson(json);

                String dest = data.get(UserID);
                if (dest != null) {
                    header = "HTTP/1.1 302 Found\r\n" +
                            "Location: " + dest + "\r\n" +
                            "Content-Length: 0\r\n" +
                            "\r\n";
                    out.write(header.getBytes());
                    out.flush();
                    System.out.println("index.html was redirected to destination.");
                } else {
                    out.write(header.getBytes());
                    out.write(fileContent);
                    out.flush();
                    System.out.println("index.html was served.");
                }
            } else {
                File file = new File("resources/index.html");
                long lastModified = file.lastModified();
                String lastModifiedString = formatDataToHTTP(lastModified);
                header += "Cache-Control: max-age=60\r\n" +
                        "Last-Modified: " + lastModifiedString + "\r\n" +
                        "\r\n";

                System.out.println("New user requested page, cookie will be set.");
                out.write(header.getBytes());
                out.write(fileContent);
                out.flush();
            }
        }
    }
}
