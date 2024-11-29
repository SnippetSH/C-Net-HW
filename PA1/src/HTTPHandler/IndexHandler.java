package HTTPHandler;

import structure.AdditionalInfo;
import helper.JsonFileHandler;

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

            String filePath = "static/index.html";
            String newUserID = makeRandomID();
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

            String header = "";
            if (cookieEnabled) {
                header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + fileContent.length + "\r\n" +
                        "Set-Cookie: UserID=" + (UserID.isEmpty() ? newUserID : UserID) + "; Path=/; Max-Age=3600\r\n" +
                        "\r\n";
            } else {
                    header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + fileContent.length + "\r\n" +
                        "\r\n";
            }


            if (!hasCookie && cookieEnabled) {
                System.out.println("New user requested page, cookie will be set.");
                out.write(header.getBytes());
                out.write(fileContent);
                out.flush();
            } else {
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
            }   
        }
    }
}
