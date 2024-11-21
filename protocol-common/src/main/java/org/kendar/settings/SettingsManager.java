package org.kendar.settings;

import org.kendar.utils.JsonMapper;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SettingsManager {
    protected static final JsonMapper mapper = new JsonMapper();

    public static GlobalSettings load(String path) throws Exception {
        var file = Path.of(path).toAbsolutePath();
        if (!file.toFile().exists()) {
            throw new FileNotFoundException(file.toString());
        }
        var content = new String(Files.readAllBytes(file));
        return mapper.deserialize(content, GlobalSettings.class);
    }
}
