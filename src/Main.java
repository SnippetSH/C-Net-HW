import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Main {
    public static void main(String[] args) throws Exception{
        String clientSentence;
        try (ServerSocket connectSocket = new ServerSocket(8080);){
            System.out.println("Server is running on 8080");

            while (true) {
                try (Socket client = connectSocket.accept()) {
                    System.out.println("Client tried to connect this server, " + client.getInetAddress());
                    handleRequest(client);
                } catch (Exception e) {
                    System.out.println("Error occurred during processing the request. " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void handleRequest(Socket client) throws Exception{
        try(
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream out = client.getOutputStream();
            PrintWriter writer = new PrintWriter(out, true);
        ) {
            String RequestLine = in.readLine();
            System.out.println("Request line is " + RequestLine);

            if (RequestLine == null || !RequestLine.startsWith("GET")) {
                System.out.println("It's not GET method.");
                return;
            }

            boolean hasCookie = false;
            String cookieVal = null;
            String line;

            while((line = in.readLine()) != null) {
                System.out.println("Header: "+ line);

                if (line.startsWith("Cookie:")) {
                    hasCookie = true;
                    cookieVal = line.substring(7).trim();

                    System.out.println("Cookie is " + cookieVal);
                    break;
                }
            }

            String[] requestPath = RequestLine.split(" ");
            String path = requestPath[1];

            String res;

            String currentPath = System.getProperty("user.dir");
//            System.out.println("current path: " + currentPath);
            String filePath = currentPath + "/src/static" + path;
            if ("/".equals(path)) {
                filePath = currentPath + "/src/static/index.html";
            }

            File file = new File(filePath);
            if (file.exists() && !file.isDirectory()) {
                String UserID = makeRandomID();
                byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
                String header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + fileContent.length + "\r\n" +
                        (hasCookie ? "" : "Set-Cookie: UserID="+UserID+"; Path=/; Max-Age=3600\r\n") +
                        "\r\n";

                out.write(header.getBytes());
                out.write(fileContent);
                out.flush();
                System.out.println("File was sent " + filePath);
            } else {
                res = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: 53\r\n" +
                        "\r\n" +
                        "<html><body><h1>404 - Page Not Found</h1></body></html>";
                writer.print(res);
                writer.flush();
                System.out.println("File was not found " + filePath);
            }
        } catch(Exception e) {
            System.out.println("Request Processing was broken with " + e.getMessage());
        }
    }
    private static String makeRandomID() {
        int[] ids = new int[10];
        String ID = "";
        for (int i = 0; i < 10; i++) {
            ids[i] = (int)(Math.random() * 10);
            ID += Integer.toString(ids[i]);
        }

        return ID;
    }
}