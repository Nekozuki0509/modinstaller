package modinstaller.modinstaller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static modinstaller.modinstaller.Filer.*;
import static modinstaller.modinstaller.Main.*;

@Getter
@Slf4j
public class MainController {

    static MainController controller;

    private static final ObjectMapper mapper = new ObjectMapper();

    @FXML
    @Getter(AccessLevel.NONE)
    private ResourceBundle resources;

    @FXML
    @Getter(AccessLevel.NONE)
    private URL location;

    @FXML
    private TextField version;

    @FXML
    private TextField input;

    @FXML
    private TextField output;

    @FXML
    private TableView<Entity> inputFilesTable;

    @FXML
    private TableView<Entity> idsTable;

    @FXML
    @Getter(AccessLevel.NONE)
    private TableColumn<Entity, Boolean> inputFilesDownloadColumn;

    @FXML
    @Getter(AccessLevel.NONE)
    private TableColumn<Entity, String> inputFilesIdColumn;

    @FXML
    private TableColumn<Entity, String> inputFilesProjectColumn;

    @FXML
    private TableColumn<Entity, String> inputFilesFileColumn;

    @FXML
    @Getter(AccessLevel.NONE)
    private TableColumn<Entity, Boolean> idsDownloadColumn;

    @FXML
    @Getter(AccessLevel.NONE)
    private TableColumn<Entity, String> idsIdColumn;

    @FXML
    private TableColumn<Entity, String> idsProjectColumn;

    @FXML
    private TableColumn<Entity, String> idsFileColumn;

    @FXML
    private ProgressBar inputFilesLoadProgress;

    @FXML
    private ProgressBar idsLoadProgress;

    @FXML
    private ProgressBar downloadProgress;

    @FXML
    private Label inputFilesLoadLabel;

    @FXML
    private Label idsLoadLabel;

    @FXML
    private Label downloadLabel;

    @FXML
    private Button downloadBtn;

    @FXML
    @Getter(AccessLevel.NONE)
    private ImageView discord;

    @FXML
    @Getter(AccessLevel.NONE)
    private TitledPane inputFilesTitledPane;

    @FXML
    @Getter(AccessLevel.NONE)
    private TitledPane idsTitledPane;

    @FXML
    @Getter(AccessLevel.NONE)
    private Button reloadBtn;

    @FXML
    @Getter(AccessLevel.NONE)
    private Button addIdBtn;

    @FXML
    @Getter(AccessLevel.NONE)
    private Button removeIdBtn;

    @FXML
    @Getter(AccessLevel.NONE)
    private GridPane controlGrid;

    @FXML
    void onFileChoose(MouseEvent e) {
        TextField field = ((TextField) e.getSource());

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("select Directory");
        directoryChooser.setInitialDirectory(new File(field.getText().isEmpty() ? System.getProperty("user.home") : field.getText()));

        File file = directoryChooser.showDialog(null);
        if (file != null) {
            field.setText(file.getAbsolutePath());
            if ("input".equals(field.getId()) && !version.getText().isEmpty()) {
                if (inputFileLoaderThread.isAlive()) {
                    inputFilesFlag.set(true);
                    try {
                        inputFileLoaderThread.join();
                    } catch (InterruptedException ex) {
                        log.error("interrupting input file loader thread", ex);
                    }
                    log.debug("input file loading was interrupted!");
                }

                inputFileLoaderThread = new Thread(() -> {
                    inputFilesTable.getItems().clear();
                    inputFilesLoader();
                });
                inputFileLoaderThread.start();
            }
        }

        e.consume();
    }

    @FXML
    void onDragOver(DragEvent e) {
        Dragboard dragboard = e.getDragboard();
        if (dragboard.hasFiles() && dragboard.getFiles().size() == 1 && dragboard.getFiles().get(0).isDirectory()) {
            e.acceptTransferModes(TransferMode.ANY);
        }

        e.consume();
    }

    @FXML
    void onFileDragDropped(DragEvent e) {
        TextField field = ((TextField) e.getSource());

        field.setText(e.getDragboard().getFiles().get(0).getAbsolutePath());
        if ("input".equals(field.getId()) && !version.getText().isEmpty()) {
            if (inputFileLoaderThread.isAlive()) {
                inputFilesFlag.set(true);
                try {
                    inputFileLoaderThread.join();
                } catch (InterruptedException ex) {
                    log.error("interrupting input file loader thread", ex);
                }
                log.debug("input file loading was interrupted!");
            }

            inputFileLoaderThread = new Thread(() -> {
                inputFilesTable.getItems().clear();
                inputFilesLoader();
            });
            inputFileLoaderThread.start();
        }
        e.consume();
    }

    @FXML
    void onMouseEntered(InputEvent e) {
        Node node = ((Node) e.getSource());
        node.setStyle(node.getStyle() + "-fx-border-color: blue;");

        e.consume();
    }

    @FXML
    void onMouseExited(InputEvent e) {
        Node node = ((Node) e.getSource());
        node.setStyle(node.getStyle().replace("-fx-border-color: blue;", ""));

        e.consume();
    }

    @FXML
    void onReload(ActionEvent e) {
        log.debug("start reloading...");

        try {
            config = mapper.readValue(getConfigf(), Config.class);
        } catch (IOException ex) {
            log.error("on reloading config", ex);
        }

        if (version.getText().isEmpty()) return;

        if (inputFileLoaderThread.isAlive()) {
            inputFilesFlag.set(true);
            try {
                inputFileLoaderThread.join();
            } catch (InterruptedException ex) {
                log.error("interrupting input file loader thread", ex);
            }
            log.debug("input file loading was interrupted!");
        }
        inputFileLoaderThread = new Thread(() -> {
            inputFilesTable.getItems().clear();
            inputFilesLoader();
        });
        inputFileLoaderThread.start();

        if (idLoaderThread.isAlive()) {
            idsFlag.set(true);
            try {
                idLoaderThread.join();
            } catch (InterruptedException ex) {
                log.error("interrupting id loader thread", ex);
            }
            log.debug("id mod loading was interrupted!");
        }
        idLoaderThread = new Thread(() -> {
            idsTable.getItems().clear();
            IDsLoader();
        });
        idLoaderThread.start();

        e.consume();
    }

    @FXML
    void onAddId(ActionEvent e) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(MainApp.class.getResource("main.css")).toExternalForm());
        dialog.setTitle("add id or slug");
        dialog.showAndWait().ifPresent(id -> {
            config.getIds().add(id);
            IDsLoader(id);
        });

        e.consume();
    }

    @FXML
    void onRemoveId(ActionEvent e) {
        Entity entity = idsTable.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("remove id or slug");
        alert.setContentText("Are you sure you want to remove the \"%s\"?".formatted(entity.getId()));
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(MainApp.class.getResource("main.css")).toExternalForm());

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            config.getIds().remove(entity.getId());
            idsTable.getItems().remove(entity);
        }

        e.consume();
    }

    @FXML
    void onDownload(ActionEvent e) {
        new Thread(() -> {
            log.debug("start downloading...");

            try {
                Platform.runLater(() -> {
                    downloadBtn.setDisable(true);
                    reloadBtn.setDisable(true);
                });

                Path file;
                if (!output.getText().isEmpty() && Files.exists(Path.of(output.getText())))
                    file = Path.of(output.getText());
                else {
                    Path outputp = Path.of("output");
                    if (Files.notExists(outputp)) {
                        try {
                            Files.createDirectory(outputp);
                        } catch (IOException ex) {
                            log.error("creating a directory", ex);
                        }
                    }
                    file = outputp;
                }

                config.getIgnore().clear();

                List<Modrinth.modrinthData> data = new java.util.ArrayList<>() {
                    {
                        addAll(inputFilesTable.getItems().stream()
                                .filter(entity -> {
                                    if (entity.getModrinthData().filename() == null) return false;
                                    if (!entity.isChecked()) {
                                        config.getIgnore().add(entity.getId());
                                        return false;
                                    }
                                    return true;
                                })
                                .map(Entity::getModrinthData)
                                .toList()
                        );
                        addAll(idsTable.getItems().stream()
                                .filter(entity -> {
                                    if (entity.getModrinthData().filename() == null) return false;
                                    if (!entity.isChecked()) {
                                        config.getIgnore().add(entity.getId());
                                        return false;
                                    }
                                    return true;
                                })
                                .map(Entity::getModrinthData)
                                .toList()
                        );
                    }
                };

                double oneProgress = 1.0 / data.size();
                double totalProgress = 0.0;
                Platform.runLater(() -> {
                    downloadProgress.setProgress(0.0);
                    downloadProgress.setStyle("");
                });

                for (Modrinth.modrinthData modrinthData : data) {
                    Platform.runLater(() -> downloadLabel.setText("downloading %s...".formatted(modrinthData.filename())));

                    Path path = file.resolve(modrinthData.filename());
                    try {
                        if (Files.notExists(path)) {
                            log.debug("start downloading `{}`...", modrinthData.filename());

                            InputStream inputStream = new URL(modrinthData.url()).openStream();
                            Files.copy(inputStream, path);
                        }
                    } catch (Exception ex) {
                        log.error("on downloading a mod `{}`", modrinthData.filename(), ex);
                    }
                    totalProgress += oneProgress;
                    double finalTotalProgress = totalProgress;
                    Platform.runLater(() -> downloadProgress.setProgress(finalTotalProgress));
                }

                Platform.runLater(() -> {
                    downloadProgress.setProgress(1.0);
                    downloadProgress.setStyle("-fx-accent: green");
                    downloadLabel.setText("");
                });

                config.setVersion(version.getText());
                config.setInputModsDir(input.getText());
                config.setOutputModsDir(file.toAbsolutePath().toString());

                try {
                    mapper.writeValue(getConfigf(), config);
                } catch (IOException ex) {
                    log.error("saving config", ex);
                }

                Platform.runLater(() -> {
                    downloadBtn.setDisable(false);
                    reloadBtn.setDisable(false);
                });
            } catch (Exception ex) {
                log.error("on downloading mods", ex);
            }

            log.debug("finish downloading");
        }).start();

        e.consume();
    }

    @FXML
    void initialize() {
        downloadProgress.prefWidthProperty().bind(controlGrid.widthProperty().subtract(400));

        try {
            discord.setImage(new Image(Objects.requireNonNull(getClass().getResource("discord.png")).openStream()));
        } catch (IOException e) {
            log.error("setting an image", e);
        }
        discord.setOnMouseClicked(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://discord.gg/352Cdy8MjV"));
                }
            } catch (IOException | URISyntaxException ex) {
                log.error("on discord event", ex);
            }
        });

        inputFilesTitledPane.setPrefHeight(Integer.MAX_VALUE);
        inputFilesTitledPane.expandedProperty().addListener(observable -> {
            if (inputFilesTitledPane.isExpanded()) inputFilesTitledPane.setPrefHeight(Integer.MAX_VALUE);
            else inputFilesTitledPane.setPrefHeight(-1);
        });
        idsTitledPane.setPrefHeight(Integer.MAX_VALUE);
        idsTitledPane.expandedProperty().addListener(observable -> {
            if (idsTitledPane.isExpanded()) idsTitledPane.setPrefHeight(Integer.MAX_VALUE);
            else idsTitledPane.setPrefHeight(-1);
        });

        inputFilesDownloadColumn.prefWidthProperty().bind(inputFilesTable.widthProperty().subtract(18).multiply(0.1));
        inputFilesIdColumn.prefWidthProperty().bind(inputFilesTable.widthProperty().subtract(18).multiply(0.3));
        inputFilesProjectColumn.prefWidthProperty().bind(inputFilesTable.widthProperty().subtract(18).multiply(0.3));
        inputFilesFileColumn.prefWidthProperty().bind(inputFilesTable.widthProperty().subtract(18).multiply(0.3));

        idsDownloadColumn.prefWidthProperty().bind(idsTable.widthProperty().subtract(18).multiply(0.1));
        idsIdColumn.prefWidthProperty().bind(idsTable.widthProperty().subtract(18).multiply(0.3));
        idsProjectColumn.prefWidthProperty().bind(idsTable.widthProperty().subtract(18).multiply(0.3));
        idsFileColumn.prefWidthProperty().bind(idsTable.widthProperty().subtract(18).multiply(0.3));

        idsTable.getSelectionModel().selectedIndexProperty().addListener((obs, oldNum, newNum) -> removeIdBtn.setDisable(newNum.intValue() < 0));

        inputFilesDownloadColumn.setCellFactory(CheckBoxTableCell.forTableColumn(inputFilesDownloadColumn));
        inputFilesDownloadColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
        idsDownloadColumn.setCellFactory(CheckBoxTableCell.forTableColumn(idsDownloadColumn));
        idsDownloadColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));

        inputFilesIdColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        inputFilesIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idsIdColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        idsIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        inputFilesProjectColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean b) {
                super.updateItem(s, b);
                setGraphic(b ? null : inputFilesTable.getItems().get(getIndex()).getProject());
            }
        });
        idsProjectColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean b) {
                super.updateItem(s, b);
                setGraphic(b ? null : idsTable.getItems().get(getIndex()).getProject());
            }
        });
        inputFilesFileColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean b) {
                super.updateItem(s, b);
                setGraphic(b ? null : inputFilesTable.getItems().get(getIndex()).getFile());
            }
        });
        idsFileColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean b) {
                super.updateItem(s, b);
                setGraphic(b ? null : idsTable.getItems().get(getIndex()).getFile());
            }
        });
    }
}
