package org.kendar.cli;

import java.util.List;

public interface CommandItem {
    CommandItem getParent();

    void setParent(CommandItem parent);

    List<CommandOption> getCommandOptions();
}
