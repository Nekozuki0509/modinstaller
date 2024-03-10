package modinstaller.modinstaller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.HttpsURLConnection;
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
            System.out.println(id.asText());
            HttpURLConnection con = (HttpURLConnection) new URL("https://api.modrinth.com/v2/project/"+id.asText()
                    +"/version?loaders=[fabric]&game_versions=["+version+"]").openConnection();
            con.setRequestMethod("GET");
            con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            String url;
            if ((url=mapper.readTree(response.toString()).get("files").get(0).get("url").asText()) == null) {
                HttpURLConnection con2 = (HttpURLConnection) new URL("https://api.modrinth.com/v2/project/"+id).openConnection();
                con2.setRequestMethod("GET");
                con2.getResponseCode();
                BufferedReader in2 = new BufferedReader(new InputStreamReader(con2.getInputStream()));
                String inputLine2;
                StringBuffer response2 = new StringBuffer();
                while ((inputLine2 = in2.readLine()) != null) {
                    response2.append(inputLine2);
                }
                in2.close();
                String title;
                if ((title=mapper.readTree(response2.toString()).get("title").asText()) == null) {
                    System.out.println("Could not find any mod of id:"+id);
                } else {
                    System.out.println("Could not find "+title+" of version:"+version);
                }
            } else {

                // HTTPS接続を開く
                HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();

                // ファイルをダウンロード
                try (BufferedInputStream downin = new BufferedInputStream(conn.getInputStream());
                     FileOutputStream fileOutputStream = new FileOutputStream("mods")) {
                    byte[] dataBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = downin.read()) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                    System.out.println("fin");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 接続を閉じる
                conn.disconnect();
            }
        }
    }
}