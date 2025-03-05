package modinstaller.modinstaller;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    @Getter
    @Setter
    private String version;

    @Getter
    @Setter
    private String inputModsDir;

    @Getter
    @Setter
    private String outputModsDir;

    private List<String> ids;

    private Map<String, String> replace;

    private List<String> ignore;

    public List<String> getIds() {
        return ids == null ? (ids = new ArrayList<>()) : ids;
    }

    public Map<String, String> getReplace() {
        return replace == null ? (replace = new HashMap<>()) : replace;
    }

    public List<String> getIgnore() {
        return ignore == null ? (ignore = new ArrayList<>()) : ignore;
    }
}
