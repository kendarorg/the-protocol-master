package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.settings.ProtocolSettings;

import java.util.List;

public interface PluginCommandLineHandler {
     void setup(List<CommandOption> protocolOptions, ProtocolSettings settings);
}
