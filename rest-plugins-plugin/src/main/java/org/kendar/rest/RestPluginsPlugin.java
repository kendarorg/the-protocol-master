package org.kendar.rest;

import org.kendar.plugins.base.TPMPluginFile;
import org.pf4j.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RestPluginsPlugin extends Plugin  implements TPMPluginFile {

    private static final Logger log = LoggerFactory.getLogger(RestPluginsPlugin.class);

    @Override
    public String getTpmPluginName() {
        return "rest-plugins-plugin";
    }

    @Override
    public String getTpmPluginVersion() {
        try {
            return new String(
                    this.getClass().getResourceAsStream("/rest_plugins_plugin.version").readAllBytes()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

