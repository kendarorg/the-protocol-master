package org.kendar.settings;

import org.junit.jupiter.api.Test;

public class SettingsTest {
    @Test
    void test() throws Exception {
        var target = new SettingsManager();
        target.load("settings.json");
    }
}
