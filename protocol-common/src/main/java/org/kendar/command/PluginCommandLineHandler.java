package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.ExtensionPoint;

import java.util.List;

public interface PluginCommandLineHandler extends ExtensionPoint {
    void setup(List<CommandOption> protocolOptions, ProtocolSettings settings);
}
