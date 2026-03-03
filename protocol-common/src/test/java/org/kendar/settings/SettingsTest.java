package org.kendar.settings;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class SettingsTest {
    @Test
    void test() throws Exception {
        SettingsManager.load(Path.of("src", "test", "resources", "settings.json").toString());
    }
}
