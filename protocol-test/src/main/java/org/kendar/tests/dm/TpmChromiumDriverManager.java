package org.kendar.tests.dm;

import io.github.bonigarcia.wdm.managers.ChromiumDriverManager;
import org.openqa.selenium.Capabilities;

public class TpmChromiumDriverManager extends ChromiumDriverManager {
    public Capabilities retrieveCapabilities() {
        return getCapabilities();
    }
}
