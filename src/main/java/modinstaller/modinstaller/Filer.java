package modinstaller.modinstaller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static modinstaller.modinstaller.Main.*;
import static modinstaller.modinstaller.MainController.controller;
import static modinstaller.modinstaller.Modrinth.checkMod;
import static modinstaller.modinstaller.Modrinth.search;

@Slf4j
public class Filer {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final AtomicBoolean inputFilesFlag = new AtomicBoolean();

    public static final AtomicBoolean idsFlag = new AtomicBoolean();

    public static void inputFilesLoader() {
        log.debug("start input file loading...");

        try {
            Platform.runLater(() -> controller.getDownloadBtn().setDisable(true));

            Path bMods = Path.of(controller.getInput().getText());
            if (Files.exists(bMods) && new File(String.valueOf(bMods)).isDirectory()) {
                ProgressBar progressBar = controller.getInputFilesLoadProgress();
                File[] mods = Objects.requireNonNull(bMods.toFile().listFiles());
                double oneProgress = 1.0 / mods.length;
                double totalProgress = 0.0;

                Platform.runLater(() -> {
                    progressBar.setProgress(0.0);
                    progressBar.setStyle("");
                });

                for (File file : mods) {
                    try {
                        if (inputFilesFlag.getAndSet(false)) return;

                        if (!file.getName().endsWith(".jar")) continue;

                        JarFile jf = new JarFile(file);
                        JarEntry entry = jf.getJarEntry("fabric.mod.json");
                        if (entry == null) continue;

                        StringBuilder modJ = new StringBuilder();
                        BufferedReader is = new BufferedReader(new InputStreamReader(jf.getInputStream(entry)));
                        String line;
                        while ((line = is.readLine()) != null) {
                            modJ.append(line);
                        }
                        jf.close();

                        JsonNode jsonNode = mapper.readTree(modJ.toString());
                        String id = jsonNode.get("id").asText();
                        if (config.getReplace() != null) {
                            id = config.getReplace().getOrDefault(id, id);
                        }

                        String name = jsonNode.get("name") != null ? jsonNode.get("name").asText() : null;

                        String finalId = id;
                        Platform.runLater(() -> controller.getInputFilesLoadLabel().setText("checking %s...".formatted(name != null ? name : finalId)));

                        Modrinth.modrinthData mod = checkMod(id);
                        if (mod.id() == null && name != null) {
                            mod = checkMod(name);
                        }
                        if (mod.id() == null && name != null) {
                            mod = search(name);
                        }
                        if (mod.id() == null) {
                            mod = search(id);
                        }

                        String finalId1 = id;
                        Modrinth.modrinthData finalMod = mod;
                        Entity entity = new Entity(Entity.Type.INPUT_FILES, finalId1, finalMod);
                        Platform.runLater(() -> controller.getInputFilesTable().getItems().add(entity));

                        totalProgress += oneProgress;
                        double finalTotalProgress = totalProgress;
                        Platform.runLater(() -> progressBar.setProgress(finalTotalProgress));
                    } catch (Exception e) {
                        log.error("on loading a input mod `{}`", file.getName(), e);
                    }
                }

                Platform.runLater(() -> {
                    progressBar.setProgress(1.0);
                    progressBar.setStyle("-fx-accent: green");
                    controller.getInputFilesLoadLabel().setText("");
                    controller.getDownloadBtn().setDisable(idLoaderThread.isAlive());
                });
            }
        } catch (Exception e) {
            log.error("on loading input mods", e);
        }

        log.debug("finish input file loading");
    }

    public static void IDsLoader(String... ids) {
        log.debug("start id mod loading...");

        try {
            Platform.runLater(() -> controller.getDownloadBtn().setDisable(true));

            if (ids.length == 0) ids = config.getIds().toArray(String[]::new);

            ProgressBar progressBar = controller.getIdsLoadProgress();
            double oneProgress = 1.0 / ids.length;
            double totalProgress = 0.0;

            Platform.runLater(() -> {
                progressBar.setProgress(0.0);
                progressBar.setStyle("");
            });

            for (String id : ids) {
                try {
                    if (idsFlag.getAndSet(false)) return;

                    Entity entity = new Entity(Entity.Type.IDS, id, checkMod(id));
                    Platform.runLater(() -> {
                        controller.getIdsLoadLabel().setText("checking %s...".formatted(id));
                        controller.getIdsTable().getItems().add(entity);
                    });

                    totalProgress += oneProgress;
                    double finalTotalProgress = totalProgress;
                    Platform.runLater(() -> progressBar.setProgress(finalTotalProgress));
                } catch (Exception e) {
                    log.error("on loading a id mod `{}`", id, e);
                }
            }

            Platform.runLater(() -> {
                progressBar.setProgress(1.0);
                progressBar.setStyle("-fx-accent: green");
                controller.getIdsLoadLabel().setText("");
                controller.getDownloadBtn().setDisable(inputFileLoaderThread.isAlive());
            });
        } catch (Exception e) {
            log.error("on loading id mods", e);
        }

        log.debug("finish id mod loading");
    }
}
