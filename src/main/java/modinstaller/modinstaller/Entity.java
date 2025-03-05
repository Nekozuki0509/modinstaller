package modinstaller.modinstaller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Hyperlink;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.net.URI;
import java.util.Map;

import static modinstaller.modinstaller.Main.config;
import static modinstaller.modinstaller.MainController.controller;
import static modinstaller.modinstaller.Modrinth.checkMod;
import static modinstaller.modinstaller.Modrinth.search;

@Slf4j
public class Entity {

    private final SimpleBooleanProperty checked;

    private final SimpleStringProperty id;

    @Getter
    private final Hyperlink project = new Hyperlink();

    @Getter
    private final Hyperlink file = new Hyperlink();

    @Getter
    private Modrinth.modrinthData modrinthData;

    public Entity(Type type, String id, Modrinth.modrinthData modrinthData) {
        this.id = new SimpleStringProperty(id);
        this.checked = new SimpleBooleanProperty();
        this.modrinthData = modrinthData;

        this.id.addListener((obs, oldS, newS) -> {
            if (type == Type.INPUT_FILES) {
                if (config.getReplace().containsValue(oldS)) {
                    String key = "";
                    for (Map.Entry<String, String> mapEntry : config.getReplace().entrySet()) {
                        if (mapEntry.getValue().equals(oldS)) key = mapEntry.getKey();
                        break;
                    }
                    if (key.equals(newS)) config.getReplace().remove(key);
                    else config.getReplace().put(key, newS);
                } else config.getReplace().put(oldS, newS);

                this.modrinthData = checkMod(newS);
                if (this.modrinthData.id() == null) {
                    this.modrinthData = search(newS);
                }
            } else {
                config.getIds().remove(oldS);
                config.getIds().add(newS);
                this.modrinthData = checkMod(newS);
            }

            setLink();
        });

        setLink();
    }

    public void setLink() {
        if (modrinthData.title() == null) {
            project.setText("NOT FOUND");
            project.setStyle("-fx-text-fill: red;");
            log.warn("[PROJECT NOT FOUND] modrinth project `{}` was not found!", id.get());
        } else {
            project.setText(modrinthData.title());
            project.setOnAction(e -> {
                if (modrinthData.id() == null) return;
                try {
                    Desktop.getDesktop().browse(new URI("https://modrinth.com/mod/%s".formatted(modrinthData.id())));
                } catch (Exception ex) {
                    log.error("on project browse `{}`", modrinthData.title(), ex);
                }

                e.consume();
            });
            project.setStyle("-fx-text-fill: white;");
        }

        if (modrinthData.filename() == null) {
            file.setText("NOT FOUND");
            file.setStyle("-fx-text-fill: red;");
            if (modrinthData.title() != null)
                log.warn("[FILE NOT FOUND] `{}({})` of version `{}` was not found!", modrinthData.title(), id.get(), controller.getVersion().getText());
        } else {
            file.setText(modrinthData.filename());
            file.setOnAction(e -> {
                if (modrinthData.filename() == null) return;
                try {
                    Desktop.getDesktop().browse(new URI("https://modrinth.com/mod/%s/version/%s".formatted(modrinthData.id(), modrinthData.version())));
                } catch (Exception ex) {
                    log.error("on file browse `{}`", modrinthData.filename(), ex);
                }

                e.consume();
            });
            file.setStyle("-fx-text-fill: white;");
            if (config.getIgnore().contains(id.get())) {
                log.warn("[IGNORED] `{}({})` was ignored!!", modrinthData.title(), modrinthData.id());
                this.checked.set(false);
            } else this.checked.set(true);
        }
    }

    public boolean isChecked() {
        return checked.get();
    }

    public SimpleBooleanProperty checkedProperty() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked.set(checked);
    }

    public String getId() {
        return id.get();
    }

    public SimpleStringProperty idProperty() {
        return id;
    }

    public void setId(String id) {
        this.id.set(id);
    }

    enum Type {
        INPUT_FILES,
        IDS
    }
}
