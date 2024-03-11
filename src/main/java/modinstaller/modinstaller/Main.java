package modinstaller.modinstaller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Main {
    public static void main(String[] args) throws IOException {
        Path config = Path.of("config.json");
        if (Files.notExists(config))
            try {
                Files.copy(Objects.requireNonNull(Main.class.getResourceAsStream("/config.json")), config);
                System.out.println("I made a config file");
                System.out.println("please edit");

            } catch (IOException e) {
                throw new RuntimeException("could not make config file!");
            }
        Path mods = Path.of("mods");
        if (Files.notExists(mods))
            try {
                Files.createDirectory(mods);
            } catch (IOException e) {
                throw new RuntimeException("could not make mods directory!");
            }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(new File("config.json"));
        String version = json.get("version").asText();
        for (JsonNode id : json.get("ids")){
            HttpURLConnection con = (HttpURLConnection) new URL("https://api.modrinth.com/v2/project/"+id.asText()
                    +"/version?loaders=[%22fabric%22]&game_versions=[%22"+version+"%22]").openConnection();

            con.setRequestMethod("GET");

            BufferedReader in;
            try {
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } catch (FileNotFoundException e){
                System.out.println("Could not find any mod of id:"+id.asText());
                continue;
            }

            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JsonNode res = mapper.readTree(response.toString()).get(0);JsonNode files;
            Path file_p;
            try {
                if (Files.notExists(file_p = mods.resolve((files = res.get("files").get(0)).get("filename").asText()))) {
                    try (InputStream downin = new URL(files.get("url").asText()).openStream()) {
                        Files.copy(downin, file_p);
                    }
                } else {
                    System.out.println(files.get("filename").asText() + " is already exists");
                }
            } catch (NullPointerException e) {
                HttpURLConnection con2 = (HttpURLConnection) new URL("https://api.modrinth.com/v2/project/"+id.asText()).openConnection();

                con2.setRequestMethod("GET");

                BufferedReader in2 = new BufferedReader(new InputStreamReader(con2.getInputStream()));
                String inputLine2;
                StringBuffer response2 = new StringBuffer();
                while ((inputLine2 = in2.readLine()) != null) {
                    response2.append(inputLine2);
                }
                in2.close();
                System.out.println("Could not find any "+mapper.readTree(response2.toString()).get("title")+" of version:"+version);
            }
        }
    }
}