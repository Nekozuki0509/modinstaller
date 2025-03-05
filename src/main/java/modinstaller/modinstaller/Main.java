package modinstaller.modinstaller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public class Main {

    @Getter
    private static final File configf = Path.of("config.json").toFile();

    static Config config;

    static Thread idLoaderThread;

    static Thread inputFileLoaderThread;

    public static void main(String[] args) {
        try {
            if (Files.notExists(configf.toPath())) {
                try {
                    Files.copy(Objects.requireNonNull(Main.class.getResourceAsStream("/config.json")), Path.of("config.json"));
                } catch (IOException e) {
                    log.error("creating config", e);
                }
            }

            config = new ObjectMapper().readValue(configf, Config.class);

            Application.launch(MainApp.class);
        } catch (Exception e) {
            log.error("init", e);
        }
    }
}