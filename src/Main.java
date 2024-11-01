import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

public class Main {
    public static void main(String[] args) throws Exception{
        JsonFileHandler.writeJson("data.json", "", "");
        Gson gson = new Gson();
        ArrayList<StructureDest> dest = null;

        try (FileReader reader = new FileReader("static/destinations.json")) {
            DestinationArray destinationArray = gson.fromJson(reader, DestinationArray.class);
            dest = destinationArray.getDestinations();
        } catch (Exception e) {
            System.out.println("An error occurred while parsing destinations.json." + e.getMessage());
        }

        try (ServerSocket connectSocket = new ServerSocket(8080);){
            System.out.println("Server is running on 8080");

            while (true) {
                try (Socket client = connectSocket.accept()) {
                    System.out.println("Client tried to connect this server, " + client.getInetAddress());

                    handler(client, dest);
                } catch (Exception e) {
                    System.out.println("Error occurred during processing the request. " + e.getMessage());
                }
            }
        } catch (NullPointerException e) {
            System.out.println("Cannot Parsing destinations." + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handler(Socket client, ArrayList<StructureDest> dest) throws Exception {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream out = client.getOutputStream();
            PrintWriter writer = new PrintWriter(out, true);
        ) {
            String req = in.readLine();
            if (req == null || !req.startsWith("GET")) {
                System.out.println("It's not GET method");
                String res = "HTTP/1.1 405 Method Not Allowed\r\n" +
                        "Allow: GET\r\n" +
                        "Content-Length: 0\r\n" +
                        "\r\n";
                writer.print(res);
                writer.flush();
                return;
            }

            String[] reqs = req.split(" ");
            String[] destinationsType = {"/mountains", "/beach", "/city"};
            ArrayList<String> destinationsName = new ArrayList<>();
            for (StructureDest dst : dest) {
                destinationsName.add(dst.getName().replace(" ", ""));
            }

            boolean found1 = Arrays.asList(destinationsType).contains(reqs[1]);
            boolean found2 = destinationsName.contains(reqs[1].replace("/", ""));

            String line, UserID = "";
            boolean hasCookie = false;

            while((line = in.readLine()) != null) {
                // System.out.println("Header: "+ line);

                if (line.startsWith("Cookie:")) {
                    UserID = line.substring(15).trim();

                    if (!UserID.isEmpty()) {
                        hasCookie = true;
                        System.out.println("Returning user, UserId: " + UserID);
                    }
                    break;
                }

                if (line.isEmpty() || line.isBlank()) {
                    break;
                }
            }

            if (reqs[1].equals("/")) {
                IndexHandler(out, UserID, hasCookie);
                return;
            } else if (found1) {
                DestinationHandler(out, UserID, hasCookie, reqs[1], dest);
                return;
            } else if (found2) {
                DetailHandler(out, UserID, hasCookie, reqs[1], dest);
                return;
            } else {
                if (!reqs[1].endsWith(".jpg")) {
                    String header = "HTTP/1.1 404 Not Found\r\n" +
                            "\r\n";
                    out.write(header.getBytes());
                    out.flush();
                    return;
                }

                byte[] fileContent = Files.readAllBytes(Paths.get(reqs[1].replaceFirst("^/", "")));
                String header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: image/jpeg\r\n" +
                        "Content-Length: " + fileContent.length + "\r\n" +
                        "\r\n";
                out.write(header.getBytes());
                out.write(fileContent);
                out.flush();
                return;
            }
        } catch (Exception e) {
            System.out.println("An error occurred at handler with : " + e.getMessage());
        }
    }

    private static void IndexHandler(
        OutputStream out,
        String UserID,
        boolean hasCookie
    ) throws Exception {
        String filePath = "static/index.html";
        String newUserID = makeRandomID();
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        String header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + fileContent.length + "\r\n" +
                "Set-Cookie: UserID=" + (UserID.isEmpty() ? newUserID : UserID) + "; Path=/; Max-Age=3600\r\n" +
                "\r\n";

        if (!hasCookie) {
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
            } else {
                out.write(header.getBytes());
                out.write(fileContent);
                out.flush();
            }
        }
    }

    private static void DestinationHandler(
        OutputStream out,
        String UserID,
        boolean hasCookie,
        String dest,
        ArrayList<StructureDest> destinations
    ) throws Exception { // body
        ArrayList<String> names = new ArrayList<>();
        for (StructureDest dst : destinations) {
            if (dst.getType().equals(dest.replace("/", ""))) {
                names.add(dst.getName());
            }
        }

        if (!hasCookie) {
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

        String header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + fileContent.length + "\r\n" +
                "Set-Cookie: UserID=" + UserID + "; Path=/; Max-Age=3600\r\n" +
                "\r\n";

        out.write(header.getBytes());
        out.write(fileContent);
        out.flush();
    }

    private static void DetailHandler(
        OutputStream out,
        String UserID,
        boolean hasCookie,
        String detail,
        ArrayList<StructureDest> destinations
    ) throws Exception { // body
        StructureDest used = null;
        for (StructureDest dst : destinations) {
            if ((detail.replace("/", "")).equals(dst.getName().replace(" ", ""))) {
                used = new StructureDest(dst);
            }
        }

        if (!hasCookie) {
            UserID = makeRandomID();
            System.out.println("New user requested page, cookie will be set.");
        }

        if (used == null) {
            throw new NullPointerException("Parsed data is invalid");
        }

        System.out.println(used.getName());
        JsonFileHandler.writeJson("data.json", UserID, used.getType());

        String filePath = "static/detail.html";
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        String content = new String(fileContent, StandardCharsets.UTF_8);

        content = content.replace("TRAVEL DESTINATION TITLE", used.getName());
        content = content.replace("TRAVEL DESTINATION TYPE", used.getType());
        content = content.replace("TRAVEL DESTINATION DESCRIPTION", used.getDescription());
        content = content.replace("IMAGE SRC", "static/"+used.getImage().replace("//", "/"));

        fileContent = content.getBytes();

        String header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + fileContent.length + "\r\n" +
                "Set-Cookie: UserID=" + UserID + "; Path=/; Max-Age=3600\r\n" +
                "\r\n";

        out.write(header.getBytes());
        out.write(fileContent);
        out.flush();
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