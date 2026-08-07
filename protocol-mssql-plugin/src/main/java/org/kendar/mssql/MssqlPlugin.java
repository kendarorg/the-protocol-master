package org.kendar.mssql;

import org.kendar.plugins.base.TPMPluginFile;
import org.pf4j.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * pf4j entry point (manifest {@code Plugin-Class}) for the MSSQL protocol,
 * packaged as a plugin jar per protocol.pluginization.md.
 */
public class MssqlPlugin extends Plugin implements TPMPluginFile {

    private static final Logger log = LoggerFactory.getLogger(MssqlPlugin.class);

    @Override
    public void start() {
        log.info("MssqlPlugin.start()");
    }

    @Override
    public void stop() {
        log.info("MssqlPlugin.stop()");
    }

    @Override
    public void delete() {
        log.info("MssqlPlugin.delete()");
    }

    @Override
    public String getTpmPluginName() {
        return "protocol-mssql-plugin";
    }

    @Override
    public String getTpmPluginVersion() {
        try {
            return new String(
                    this.getClass().getResourceAsStream("/protocol_mssql_plugin.version").readAllBytes()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
