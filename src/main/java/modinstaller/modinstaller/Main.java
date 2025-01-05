package modinstaller.modinstaller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main {
    static String version;
    static ObjectMapper mapper = new ObjectMapper();
    static Path output = Path.of("output");

    public static void main(String[] args) throws IOException {
        Path config = Path.of("config.json");
        if (Files.notExists(config))
            try {
                Files.copy(Objects.requireNonNull(Main.class.getResourceAsStream("/config.json")), config);
                System.out.println("I made a config file");
                System.out.println("please edit");
                System.exit(0);
            } catch (IOException e) {
                throw new RuntimeException("could not make config file!");
            }

        if (Files.notExists(output))
            try {
                Files.createDirectory(output);
            } catch (IOException e) {
                throw new RuntimeException("could not make output directory!");
            }

        JsonNode configj = mapper.readTree(new File("config.json"));
        version = configj.get("version").asText();

        Path bMods;
        if (Files.exists((bMods = Path.of(configj.get("baseModsDir").asText())))) {
            for (File file : Objects.requireNonNull(bMods.toFile().listFiles())) {
                if (!file.getName().endsWith(".jar")) continue;

                try (JarFile jf = new JarFile(file)) {
                    JarEntry entry = jf.getJarEntry("fabric.mod.json");
                    if (entry != null) {
                        StringBuffer mod = new StringBuffer();
                        try (BufferedReader is = new BufferedReader(new InputStreamReader(jf.getInputStream(entry)))) {
                            String line;
                            while ((line = is.readLine()) != null) {
                                mod.append(line);
                            }
                        }

                        download(mapper.readTree(mod.toString()).get("id").asText());
                    }
                }
            }
        }

        for (JsonNode id : configj.get("ids")) {
            download(id.asText());
        }
    }

    public static void download(String id) throws IOException {

        id = id.replace(" ", "-").replace("_", "-");

        HttpURLConnection con = (HttpURLConnection) new URL("https://api.modrinth.com/v2/project/" + id
                + "/version?loaders=[%22fabric%22]&game_versions=[%22" + version + "%22]").openConnection();

        con.setRequestMethod("GET");

        StringBuffer response = new StringBuffer();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not find any mod of id : \"" + id + "\"");
            return;
        }

        Path file_p;
        try {
            JsonNode file = mapper.readTree(response.toString()).get(0).get("files");
            int i = 0;
            if (file.has(1)) {
                for (JsonNode jar : file) {
                    if (jar.get("filename").asText().contains(version)) break;
                    i++;
                }
            }
            JsonNode files;
            if (Files.notExists(file_p = output.resolve((files = file.get(i)).get("filename").asText()))) {
                try (InputStream downin = new URL(files.get("url").asText()).openStream()) {
                    Files.copy(downin, file_p);
                }
            } else {
                System.out.println(files.get("filename").asText() + " is already exists");
            }
        } catch (NullPointerException e) {
            HttpURLConnection con2 = (HttpURLConnection) new URL("https://api.modrinth.com/v2/project/" + id).openConnection();

            con2.setRequestMethod("GET");

            BufferedReader in2 = new BufferedReader(new InputStreamReader(con2.getInputStream()));
            String inputLine2;
            StringBuffer response2 = new StringBuffer();
            while ((inputLine2 = in2.readLine()) != null) {
                response2.append(inputLine2);
            }
            in2.close();
            System.out.println("Could not find any " + mapper.readTree(response2.toString()).get("title") + " of version : " + version);
        }
    }
}