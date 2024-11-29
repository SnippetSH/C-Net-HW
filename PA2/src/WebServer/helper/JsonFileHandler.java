package WebServer.helper;

import java.io.*;
import java.nio.*;
import java.util.*;

public class JsonFileHandler {
    public static void writeJson(String filePath, String key, String value) {
        StringBuilder jsonContent = new StringBuilder();
        jsonContent.append("{\n");

        int cnt = 0;
        if (!key.isEmpty() && !value.isEmpty()) {
            String json = readJson(filePath);
            Map<String, String> data = parseJson(json);
            if (data.isEmpty()) {
                data = new HashMap<>();
            }
            data.put(key, value);
            for (Map.Entry<String, String> entry : data.entrySet()) {
                jsonContent.append("\t \"").append(entry.getKey()).append("\": \"")
                        .append(entry.getValue()).append("\"");
                if (++cnt < data.size()) {
                    jsonContent.append(",");
                }
                jsonContent.append("\n");
            }
        }

        jsonContent.append("}");

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(jsonContent.toString());
        } catch (Exception e) {
            System.out.println("An error occurred during writing file." + e.getMessage());
        }
    }

    public static String readJson(String filePath) {
        StringBuilder jsonContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);

                if (line.isBlank() || line.isEmpty()) {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            writeJson(filePath, "", "");
            return null;
        } catch (Exception e) {
            System.out.println("An error occurred during reading file." + e.getMessage());
        }

        return jsonContent.toString();
    }

    public static Map<String, String> parseJson(String json) {
        Map<String, String> result = new HashMap<>();

        json = json.trim();
        if (json.equals("{}")) {
            return Collections.emptyMap();
        }
        json = json.substring(1, json.length() - 1);

        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] keyAndValue = pair.split(":");
            String key = keyAndValue[0].trim().replace("\"", "");
            String value = keyAndValue[1].trim().replace("\"", "");

            result.put(key, value);
        }

        return result;
    }
}
