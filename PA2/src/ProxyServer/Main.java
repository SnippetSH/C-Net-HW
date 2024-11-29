package ProxyServer;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    private static final int PROXY_PORT = 8085;
    private static final Map<String, CachedResponse> cache = new HashMap<>();

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(PROXY_PORT);
        System.out.println("Server started on port " + PROXY_PORT);

        try {
            while (true) {
                Socket client = server.accept();
                handleClientRequest(client);
            }
        } catch (Exception e) {
            System.out.println("Server stopped" + e.getMessage());
        } finally {
            server.close();
        }

    }

    private static void handleClientRequest(Socket client) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        ) {
            String req = in.readLine();
            System.out.println("Request: " + req);

            if (req == null || !req.startsWith("GET")) {
                return;
            }

            // Extract the requested path
            String[] requestParts = req.split(" ");
            if (requestParts.length < 2) {
                return;
            }
            String path = requestParts[1];

            String clientLine;
            String cookie = "";
            while ((clientLine = in.readLine()) != null && !clientLine.isEmpty()) {
                if (clientLine.contains("Cookie:")) {
                    cookie = clientLine.substring("Cookie: UserID=".length()).trim();
                }
            }
            System.out.println("Client's Cookie: "+cookie);

            boolean isAllowed = false;
            String[] allowedPathList = {
                    "/mountains",
                    "/beach",
                    "/city",
                    "/Hawaii",
                    "/SwissAlps",
                    "/Paris",
                    "/Maldives",
                    "/RockyMountains",
                    "/NewYork"
            };
            for (String pathElement : allowedPathList) {
                if (pathElement.equals(path)) {
                    isAllowed = true;
                    break;
                }
            }
            if (isAllowed) {
                System.out.println("client requested to allowed path, like destination or details: " + path);
            } else if (path.endsWith(".jpg")) {
                System.out.println("client requested to jpeg image: " + path);
                try (
                    Socket webServerSocket = new Socket("127.0.0.1", 8080);
                    PrintWriter writer = new PrintWriter(webServerSocket.getOutputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(webServerSocket.getInputStream()));
                    InputStream webServerInputStream = webServerSocket.getInputStream();
                ) {
                    writer.println("GET " + path + " HTTP/1.1");
                    writer.println("Host: 127.0.0.1");
                    writer.println();
                    writer.flush();

                    String statusLine = reader.readLine();
                    StringBuilder responseHeader = new StringBuilder();

                    System.out.println("Status Line: " + statusLine);
                    if (statusLine != null && statusLine.contains("200 OK")) {
                        responseHeader.append(statusLine).append("\r\n");
                        String line;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) {
                            responseHeader.append(line).append("\r\n");
                        }
                        responseHeader.append("\r\n"); // separate header and body

                        client.getOutputStream().write(responseHeader.toString().getBytes());

                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = webServerInputStream.read(buffer)) != -1) {
                            client.getOutputStream().write(buffer, 0, bytesRead);
                        }

                        client.getOutputStream().flush();
                    }

                    return;
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }
            } else if (!"/index.html".equals(path) && !"/".equals(path)) {
                return;
            }

            CachedResponse cachedResponse = cache.get(path);
            String statusLine;
            StringBuilder responseData = new StringBuilder();
            String lastModified = null;

            try (
                Socket webServerSocket = new Socket("127.0.0.1", 8080);
                PrintWriter writer = new PrintWriter(webServerSocket.getOutputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(webServerSocket.getInputStream()));
            ) {
                // Send request to the web server
                writer.println("GET " + path + " HTTP/1.1");
                writer.println("Host: 127.0.0.1");
                if (cachedResponse != null) {
                    // Send If-Modified-Since header
                    System.out.println("Checking If-Modified-Since");
                    String httpDate = formatDateToHttp(cachedResponse.getModified());
                    writer.println("If-Modified-Since: " + httpDate);
                } else {
                    System.out.println("No cached response");
                }
                if (!cookie.isEmpty()) {
                    writer.println("Cookie: UserID=" + cookie);
                }
                writer.println();
                writer.flush();

                // Read status line
                statusLine = reader.readLine();
                System.out.println("Status Line: " + statusLine);

                if (statusLine != null && statusLine.contains("304 Not Modified")) {
                    // Resource not modified, send cached response
                    System.out.println("Resource not modified, sending cached response");
                    out.print(cachedResponse.getData());
                    out.println();
                    out.flush();
                } else if (statusLine != null && statusLine.contains("200 OK")) {
                    // Read headers
                    String line;
                    boolean hasModified = false;
                    responseData.append(statusLine).append("\r\n");
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        if (line.startsWith("Last-Modified:")) {
                            lastModified = line.substring("Last-Modified:".length()).trim(); // Extract Last-Modified value
                            hasModified = true;
                        }
                        responseData.append(line).append("\r\n");
                    }
                    responseData.append("\r\n"); // Header-body separator

                    // Read body
                    while ((line = reader.readLine()) != null) {
                        responseData.append(line).append("\r\n");
                    }

                    // Cache the response
                    if (lastModified != null) {
                        long modifiedTime = parseHttpDate(lastModified);
                        cache.put(path, new CachedResponse(responseData.toString(), modifiedTime));
                    }

                    // Send response to client
                    out.print(responseData.toString());
                    out.println();
                    out.flush();

                    if (hasModified) {
                        System.out.println("Caching WebServer's response");
                    } else {
                        System.out.println("No cached response because it looks personalized page");
                    }
                } else if (statusLine != null && statusLine.contains("302 Found")) {
                    System.out.println("Resource was found by cookie, sending 302 Found response");
                    responseData.append(statusLine).append("\r\n");
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        responseData.append(line).append("\r\n");
                    }
                    responseData.append("\r\n");

                    out.print(responseData.toString());
                    out.println();
                    out.flush();
                } else {
                    // Handle other status codes or errors
                    System.out.println("Received unexpected status: " + statusLine);
                    out.println(statusLine);
                    out.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String formatDateToHttp(long timestamp) {
        SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT")); // GMT 시간대 설정
        return httpDateFormat.format(new Date(timestamp));
    }

    private static long parseHttpDate(String httpDate) {
        SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        try {
            return httpDateFormat.parse(httpDate).getTime(); // 밀리초 단위 시간 반환
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    static class CachedResponse {
        private String data;
        private long modified;

        public CachedResponse(String data, long modified) {
            this.data = data;
            this.modified = modified;
        }

        public String getData() {
            return data;
        }

        public long getModified() {
            return modified;
        }
    }
}
