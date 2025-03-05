package modinstaller.modinstaller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import qupath.ui.logviewer.ui.main.LogViewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static modinstaller.modinstaller.Main.*;
import static modinstaller.modinstaller.MainController.controller;

@Slf4j
@Getter
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = fxmlLoader.load(Objects.requireNonNull(MainApp.class.getResource("main.fxml")).openStream());
            MainController.controller = fxmlLoader.getController();

            String inputDir = config.getInputModsDir();
            if (!inputDir.isEmpty() && Files.exists(Path.of(inputDir))) controller.getInput().setText(inputDir);

            String outputDir = config.getOutputModsDir();
            if (!outputDir.isEmpty() && Files.exists(Path.of(outputDir))) controller.getOutput().setText(outputDir);

            String version = config.getVersion();
            if (!version.isEmpty()) {
                controller.getVersion().setText(version);

                idLoaderThread = new Thread(Filer::IDsLoader);
                idLoaderThread.start();

                inputFileLoaderThread = new Thread(Filer::inputFilesLoader);
                inputFileLoaderThread.start();
            } else {
                idLoaderThread = new Thread();
                inputFileLoaderThread = new Thread();
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(MainApp.class.getResource("main.css")).toExternalForm());
            stage.setTitle("Mod Installer");
            stage.setScene(scene);
            stage.show();

            LogViewer logViewer = new LogViewer();
            Stage logStage = new Stage();
            Scene logScene = new Scene(logViewer, 800, 600);
            logScene.getStylesheets().add(Objects.requireNonNull(MainApp.class.getResource("dark.css")).toExternalForm());
            logStage.setScene(logScene);
            logStage.show();

            log.info("modinstaller started!!!");
        } catch (IOException e) {
            log.error("app init", e);
        }
    }


}
