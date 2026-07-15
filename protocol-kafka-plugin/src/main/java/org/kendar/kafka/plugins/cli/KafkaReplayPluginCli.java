package org.kendar.kafka.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.AsyncReplayPluginCli;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "kafka")
public class KafkaReplayPluginCli extends AsyncReplayPluginCli {
}
