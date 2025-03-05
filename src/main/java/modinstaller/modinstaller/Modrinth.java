package modinstaller.modinstaller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static modinstaller.modinstaller.MainController.controller;

@Slf4j
public class Modrinth {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static modrinthData checkMod(String id) {
        try {
            HttpURLConnection project = (HttpURLConnection) new URL("https://api.modrinth.com/v2/project/%s".formatted(URLEncoder.encode(id, StandardCharsets.UTF_8))).openConnection();
            project.setRequestMethod("GET");

            StringBuilder proRes = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(project.getInputStream()))) {
                String inputLine;
                while ((inputLine = reader.readLine()) != null) proRes.append(inputLine);
            } catch (FileNotFoundException e) {
                return id.contains(" ") || id.contains("_") ? checkMod(id.replace(" ", "-").replace("_", "-")) : new modrinthData(null, null, null, null, null);
            }

            JsonNode ProResJ = mapper.readTree(proRes.toString());

            if (!ProResJ.get("loaders").toString().contains("fabric"))
                return new modrinthData(null, null, null, null, null);

            String MId = ProResJ.get("id").asText();
            String title = ProResJ.get("title").asText();

            String version = controller.getVersion().getText();
            HttpURLConnection file = (HttpURLConnection) new URL("https://api.modrinth.com/v2/project/%s/version?loaders=[%%22fabric%%22]&game_versions=[%%22%s%%22]".formatted(MId, version)).openConnection();

            file.setRequestMethod("GET");

            StringBuilder fiRes = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String inputLine;
            while ((inputLine = reader.readLine()) != null) fiRes.append(inputLine);
            reader.close();

            JsonNode fiResJ = mapper.readTree(fiRes.toString()).get(0);

            if (fiResJ == null) return new modrinthData(MId, title, null, null, null);


            String filename = null;
            String url = null;

            Map<String, String> files = new LinkedHashMap<>() {
                {
                    fiResJ.get("files").forEach(jsonNode -> {
                        if (!jsonNode.get("filename").asText().contains("sources"))
                            put(jsonNode.get("filename").asText(), jsonNode.get("url").asText());
                    });
                }
            };

            if (files.size() == 1) {
                for (Map.Entry<String, String> entry : files.entrySet()) {
                    filename = entry.getKey();
                    url = entry.getValue();
                }
            } else {
                Map<String, String> filea = new LinkedHashMap<>() {
                    {
                        files.keySet().forEach(key -> {
                            if (key.contains(version)) put(key, files.get(key));
                        });
                    }
                };

                if (filea.size() == 1 || filea.size() == 2 || filea.size() == files.size()) {
                    for (Map.Entry<String, String> entry : filea.entrySet()) {
                        filename = entry.getKey();
                        url = entry.getValue();
                        break;
                    }
                } else {
                    String[] vers = version.split("\\.");
                    String ver = "%s.%s".formatted(vers[0], vers[1]);

                    Map<String, String> fileb = new LinkedHashMap<>() {
                        {
                            files.keySet().forEach(key -> {
                                if (key.contains(ver)) put(key, files.get(key));
                            });
                        }
                    };

                    if (fileb.size() == 1 || fileb.size() == 2 || fileb.size() == files.size()) {
                        for (Map.Entry<String, String> entry : fileb.entrySet()) {
                            filename = entry.getKey();
                            url = entry.getValue();
                            break;
                        }
                    } else {
                        for (Map.Entry<String, String> entry : files.entrySet()) {
                            filename = entry.getKey();
                            url = entry.getValue();
                            break;
                        }
                    }
                }
            }

            return new modrinthData(MId, title, fiResJ.get("id").asText(), filename, url);
        } catch (Exception e) {
            log.error("on checking a mod `{}`", id, e);
            return new modrinthData(null, null, null, null, null);
        }
    }

    public static modrinthData search(String id) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.modrinth.com/v2/search?limit=1&facets=[[%%22categories:fabric%%22]]&query=%s"
                    .formatted(URLEncoder.encode(id.replace("_", " ").replace("-", " "), StandardCharsets.UTF_8))).openConnection();

            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = reader.readLine()) != null) builder.append(inputLine);
            reader.close();

            JsonNode jsonNode = mapper.readTree(builder.toString()).get("hits").get(0);

            if (jsonNode == null) return new modrinthData(null, null, null, null, null);

            String MId = jsonNode.get("project_id").asText();
            if ("P7dR8mSH".equals(MId)) {
                return new modrinthData(null, null, null, null, null);
            } else {
                modrinthData data = checkMod(MId);
                log.warn("[MODRINTH SEARCH] `{}({})` was found by using modrinth search! Please check it's correct mod!", data.title(), id);
                return data;
            }
        } catch (Exception e) {
            log.error("on searching a mod `{}`", id, e);
            return new modrinthData(null, null, null, null, null);
        }
    }

    record modrinthData(String id, String title, String version, String filename, String url) {
    }
}
