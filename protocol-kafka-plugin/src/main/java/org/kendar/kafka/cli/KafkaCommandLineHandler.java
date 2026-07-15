package org.kendar.kafka.cli;

import org.kendar.cli.CommandOption;
import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.kafka.KafkaProtocolSettings;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

import java.util.ArrayList;
import java.util.List;

@Extension
@TpmService(tags = "kafka")
public class KafkaCommandLineHandler extends NetworkProtocolCommandLineHandler implements ExtensionPoint {

    @Override
    protected List<CommandOption> prepareCustomOptions(GlobalSettings globalSettings, ProtocolSettings genericSettings) {
        var options = new ArrayList<CommandOption>();
        var settings = (KafkaProtocolSettings) genericSettings;
        options.add(
                CommandOption.of("pa", "Advertised host rewritten into Metadata responses (default localhost)")
                        .withLong("advertisedHost")
                        .withMandatoryParameter()
                        .withCallback(settings::setAdvertisedHost));
        options.addAll(super.prepareCustomOptions(globalSettings, genericSettings));
        return options;
    }

    @Override
    protected String getConnectionDescription() {
        return "tcp://localhost:9092";
    }

    @Override
    protected String getDefaultPort() {
        return "9092";
    }

    @Override
    public String getId() {
        return "kafka";
    }

    @Override
    public String getDescription() {
        return "Apache Kafka Protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new KafkaProtocolSettings();
    }
}
