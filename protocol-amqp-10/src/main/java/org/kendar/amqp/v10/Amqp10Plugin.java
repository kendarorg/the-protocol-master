package org.kendar.amqp.v10;

import org.kendar.plugins.base.TPMPluginFile;
import org.pf4j.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * pf4j entry point (manifest {@code Plugin-Class}) for the AMQP 1.0 protocol,
 * packaged as a plugin jar per protocol.pluginization.md.
 */
public class Amqp10Plugin extends Plugin implements TPMPluginFile {

    private static final Logger log = LoggerFactory.getLogger(Amqp10Plugin.class);

    @Override
    public void start() {
        log.info("Amqp10Plugin.start()");
    }

    @Override
    public void stop() {
        log.info("Amqp10Plugin.stop()");
    }

    @Override
    public void delete() {
        log.info("Amqp10Plugin.delete()");
    }

    @Override
    public String getTpmPluginName() {
        return "protocol-amqp-10-plugin";
    }

    @Override
    public String getTpmPluginVersion() {
        try {
            return new String(
                    this.getClass().getResourceAsStream("/protocol_amqp_10_plugin.version").readAllBytes()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
