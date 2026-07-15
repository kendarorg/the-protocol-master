package org.kendar.kafka.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.AsyncRecordPluginCli;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "kafka")
public class KafkaRecordPluginCli extends AsyncRecordPluginCli {
}
