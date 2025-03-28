package org.kendar.sample.plugin;

import org.kendar.plugins.base.TPMPluginFile;
import org.pf4j.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SamplePlugin extends Plugin implements TPMPluginFile {

    private static final Logger log = LoggerFactory.getLogger(SamplePlugin.class);

    @Override
    public void start() {
        log.info("SamplePlugin.start()");
    }

    @Override
    public void stop() {
        log.info("SamplePlugin.stop()");
    }

    @Override
    public void delete() {
        log.info("SamplePlugin.delete()");
    }

    @Override
    public String getTpmPluginName() {
        return "sample-plugins";
    }

    @Override
    public String getTpmPluginVersion() {
        try {
            return new String(
                    this.getClass().getResourceAsStream("/sample_plugins.version").readAllBytes()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

