package org.kendar.sample.plugin;

import org.pf4j.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamplePlugin extends Plugin {

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
}

