package org.kendar.dns;

import org.pf4j.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsPlugin extends Plugin {

    private static final Logger log = LoggerFactory.getLogger(DnsPlugin.class);

    @Override
    public void start() {
        log.info("DnsPlugin.start()");
    }

    @Override
    public void stop() {
        log.info("DnsPlugin.stop()");
    }

    @Override
    public void delete() {
        log.info("DnsPlugin.delete()");
    }
}

