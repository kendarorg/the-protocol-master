package org.kendar.command;

import org.kendar.cli.CommandOptions;
import org.kendar.settings.GlobalSettings;

public interface GlobalPluginCommandLineHandler {
     void setup(CommandOptions protocolOptions, GlobalSettings settings);
}
