package org.kendar.dns;

import org.kendar.plugins.base.TPMPluginFile;
import org.pf4j.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DnsPlugin extends Plugin implements TPMPluginFile {

    private static final Logger log = LoggerFactory.getLogger(DnsPlugin.class);

    @Override
    public String getTpmPluginName() {
        return "sample-plugins";
    }

    @Override
    public String getTpmPluginVersion() {
        try {
            return new String(
                    this.getClass().getResourceAsStream("/protocol_dns_plugin.version").readAllBytes()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

