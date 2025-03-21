package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;

import java.util.ArrayList;
import java.util.List;

public abstract class ProtocolCommandLineHandler {
    private List<PluginCommandLineHandler> filtersCommandLineHandlers;

    public void initializeFiltersCommandLineHandlers(List<PluginCommandLineHandler> filtersCommandLineHandlers) {
        this.filtersCommandLineHandlers = filtersCommandLineHandlers;
    }

    public abstract String getId();

    public abstract String getDescription();

    protected abstract ProtocolSettings buildProtocolSettings();

    public CommandOptions loadCommandLine(GlobalSettings globalSettings) {
        var genericSettings = buildProtocolSettings();
        var options = new ArrayList<>(prepareCustomOptions(globalSettings, genericSettings));
        for (var filter : filtersCommandLineHandlers) {
            filter.setup(options, genericSettings);
        }
        return CommandOptions.of(getId())
                .withDescription(getDescription())
                .withOptions(
                        options.toArray(new CommandOption[0])
                )
                .withCallback(s -> globalSettings.getProtocols().put(s, genericSettings));
    }

    protected List<CommandOption> prepareCustomOptions(GlobalSettings globalSettings, ProtocolSettings genericSettings) {
        return List.of();
    }
}
