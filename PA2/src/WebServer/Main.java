package WebServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

import WebServer.helper.*;
import WebServer.HTTPHandler.*;
import WebServer.structure.*;

import com.google.gson.*;

public class Main {
    static boolean cookie;
    public static void main(String[] args) throws Exception{
        int port = 8080;
        boolean cookieEnabled = true;

        try {
            port = Integer.parseInt(args[0]);

            if (args[1].startsWith("--cookie=")) {
                String cookieValue = args[1].split("=")[1];

                if (cookieValue.equalsIgnoreCase("off")) {
                    cookieEnabled = false;
                } else if (!cookieValue.equalsIgnoreCase("on")) {
                    System.out.println("Invalid cookie value. Using default value: on");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Using default port 8080.");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Invalid port number. Using default port 8080.");
        }

        cookie = cookieEnabled;
        JsonFileHandler.readJson("data.json");

        Gson gson = new Gson();
        ArrayList<StructureDest> dest = null;

        try (FileReader reader = new FileReader("resources/destinations.json")) {
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
            String ifModifiedSinceStr = null;
            while(((line = in.readLine()) != null) && cookie) {
                // System.out.println("Header: "+ line);

                if (line.startsWith("Cookie:")) {
                    UserID = line.substring(15).trim();

                    if (!UserID.isEmpty()) {
                        hasCookie = true;
                        System.out.println("Returning user, UserId: " + UserID);
                    }
                }

                if (line.startsWith("If-Modified-Since:")) {
                    ifModifiedSinceStr = line.substring("If-Modified-Since:".length()).trim();
                }

                if (line.isEmpty() || line.isBlank()) {
                    break;
                }
            }

            if (ifModifiedSinceStr != null && !hasCookie) {
                SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                try {
                    Date ifModifiedSinceDate = httpDateFormat.parse(ifModifiedSinceStr);
                    long ifModifiedSince = ifModifiedSinceDate.getTime();

                    File file = new File("resources/index.html");
                    long lastModified = file.lastModified();

                    if (ifModifiedSince < lastModified) {
                        String header = "HTTP/1.1 304 Not Modified\r\n" +
                                "Content-Length: 0\r\n" +
                                "\r\n";
                        out.write(header.getBytes());
                        out.flush();
                    }
                } catch (Exception e) {
                    System.out.println("An error occurred while parsing ifModifiedSince. " + e.getMessage());
                }
            }

            String path = reqs[1];
            boolean isDestination = isDestinationType(path);
            boolean isDetail = isDetailType(path, dest);

            System.out.println("Client requested: " + req);
            RequestHandler handler;
            if (path.equals("/") || path.equals("/index.html")) {
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
