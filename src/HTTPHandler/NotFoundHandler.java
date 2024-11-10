package HTTPHandler;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NotFoundHandler implements RequestHandler {
    @Override
    public void handle(Socket client) throws Exception {
        try (
            OutputStream out = client.getOutputStream()
        ) {
            String det = info.getDet();
            if (!det.endsWith(".jpg")) {
                String header = "HTTP/1.1 404 Not Found\r\n" +
                        "\r\n";
                out.write(header.getBytes());
                out.flush();
                return;
            }

            byte[] fileContent = Files.readAllBytes(Paths.get(det.replaceFirst("^/", "")));
            String header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/jpeg\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "\r\n";
            out.write(header.getBytes());
            out.write(fileContent);
            out.flush();
            return;
        }
    }
}
