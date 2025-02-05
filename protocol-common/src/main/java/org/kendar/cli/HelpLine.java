package org.kendar.cli;

public class HelpLine {
    private final boolean block;
    private String shortCommand;
    private String longCommand;
    private String description;
    private String availableOptions;
    private boolean multiple;

    public HelpLine(String shortCommand, String longCommand, String description, String availableOptions,boolean multiple) {
        this.shortCommand = shortCommand;
        this.longCommand = longCommand;
        this.description = description;
        this.availableOptions = availableOptions;
        this.block = false;
        this.multiple = multiple;
    }

    public HelpLine(String description) {
        this.description = description;
        this.block = true;
    }

    public boolean isBlock() {
        return block;
    }

    @Override
    public String toString() {
        if (block) {
            return description;
        } else {
            return shortCommand + "\t" + longCommand + "\t" + description;
        }
    }

    public String getShortCommand() {
        return shortCommand;
    }

    public void setShortCommand(String shortCommand) {
        this.shortCommand = shortCommand;
    }

    public String getLongCommand() {
        return longCommand;
    }

    public void setLongCommand(String longCommand) {
        this.longCommand = longCommand;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAvailableOptions() {
        return availableOptions;
    }

    public void setAvailableOptions(String availableOptions) {
        this.availableOptions = availableOptions;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }
}
