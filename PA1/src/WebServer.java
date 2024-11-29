import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import HTTPHandler.*;
import com.google.gson.*;
import helper.JsonFileHandler;
import structure.*;

public class WebServer {
    static boolean cookie;
    public static void main(String[] args) throws Exception{
        int port = 8080;
        boolean cookieEnabled = true;

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Using default port 8080.");
        }

        if (args[1].startsWith("--cookie=")) {
            String cookieValue = args[1].split("=")[1];

            if (cookieValue.equalsIgnoreCase("off")) {
                cookieEnabled = false;
            } else if (!cookieValue.equalsIgnoreCase("on")) {
                System.out.println("Invalid cookie value. Using default value: on");
            }
        }

        cookie = cookieEnabled;
        JsonFileHandler.readJson("data.json");

        Gson gson = new Gson();
        ArrayList<StructureDest> dest = null;

        try (FileReader reader = new FileReader("static/destinations.json")) {
            DestinationArray destinationArray = gson.fromJson(reader, DestinationArray.class);
            dest = destinationArray.getDestinations();
        } catch (Exception e) {
            System.out.println("An error occurred while parsing destinations.json." + e.getMessage());
        }

        try (ServerSocket connectSocket = new ServerSocket(port);){
            System.out.printf("Starting server on port %d with cookie support %s\n", port, cookieEnabled ? "enabled" : "disabled");

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

            String line, UserID = "";
            boolean hasCookie = false;

            while(((line = in.readLine()) != null) && cookie) {
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

            String path = reqs[1];
            boolean isDestination = isDestinationType(path);
            boolean isDetail = isDetailType(path, dest);

            System.out.println("Client requested: " + path);
            RequestHandler handler;
            if (path.equals("/")) {
                handler = new IndexHandler(cookie);
                handler.setAdditionalInfo(dest, hasCookie, null, UserID);
            } else if (isDestination) {
                handler = new DestinationHandler(cookie);
                handler.setAdditionalInfo(dest, hasCookie, path, UserID);
            } else if (isDetail) {
                handler = new DetailHandler(cookie);
                handler.setAdditionalInfo(dest, hasCookie, path, UserID);
            } else {
                handler = new NotFoundHandler();
                handler.setAdditionalInfo(null, false, path, null);
            }

            handler.handle(client);
        } catch (Exception e) {
            System.out.println("An error occurred at handler with : " + e.getMessage());
        }
    }

    private static boolean isDestinationType(String path) {
        String[] destinationsType = {"/mountains", "/beach", "/city"};
        return Arrays.asList(destinationsType).contains(path);
    }

    private static boolean isDetailType(String path, ArrayList<StructureDest> dest) {
        ArrayList<String> destinationsName = new ArrayList<>();
        for (StructureDest dst : dest) {
            destinationsName.add(dst.getName().replace(" ", ""));
        }

        return destinationsName.contains(path.replace("/", ""));
    }
}