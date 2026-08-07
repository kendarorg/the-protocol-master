package org.kendar.amqp.v10.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.AsyncRecordPluginCli;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "amqp10")
public class Amqp10RecordPluginCli extends AsyncRecordPluginCli {

}
