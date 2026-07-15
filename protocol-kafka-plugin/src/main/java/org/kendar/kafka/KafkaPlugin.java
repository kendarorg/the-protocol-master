package org.kendar.kafka;

import org.kendar.plugins.base.TPMPluginFile;
import org.pf4j.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * pf4j entry point (manifest {@code Plugin-Class}) for the Apache Kafka protocol,
 * packaged as a plugin jar per protocol.pluginization.md.
 */
public class KafkaPlugin extends Plugin implements TPMPluginFile {

    private static final Logger log = LoggerFactory.getLogger(KafkaPlugin.class);

    @Override
    public void start() {
        log.info("KafkaPlugin.start()");
    }

    @Override
    public void stop() {
        log.info("KafkaPlugin.stop()");
    }

    @Override
    public void delete() {
        log.info("KafkaPlugin.delete()");
    }

    @Override
    public String getTpmPluginName() {
        return "protocol-kafka-plugin";
    }

    @Override
    public String getTpmPluginVersion() {
        try {
            return new String(
                    this.getClass().getResourceAsStream("/protocol_kafka_plugin.version").readAllBytes()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
