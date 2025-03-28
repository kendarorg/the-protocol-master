package org.kendar;

import java.io.IOException;

public class VersionChecker {
    public static String getTpmVersion(){
        try {
            return new String(
                    VersionChecker.class.getResourceAsStream("/protocol_runner.version").readAllBytes()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
