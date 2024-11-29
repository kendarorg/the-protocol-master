package org.kendar.cli;

import java.util.*;
import java.util.function.Consumer;

import static org.kendar.cli.CommandOptions.parseListOfCommands;
import static org.kendar.cli.CommandOptions.printHelpListOfCommands;

public class CommandOption implements CommandItem {
    private final String shortCommand;
    private final String description;
    private final List<CommandOptions> subChoices = new ArrayList<>();
    private final Map<String, CommandOptions> subChoicesValues = new HashMap<>();
    private final Map<String, CommandOption> subOptionsValues = new HashMap<>();
    private final HashSet<String> duplicateGuardSubChoices = new HashSet<>();
    private final HashSet<String> duplicateGuardSubOptions = new HashSet<>();
    private boolean hasParameter;
    private boolean mandatoryParameter;
    private String longCommand;
    private List<String> values;
    private boolean multipleSubChoices;
    private Consumer<String> callback;
    private boolean present;
    private String subChoicesDescription;
    private final List<CommandOption> subOptions = new ArrayList<>();
    private CommandItem parent;

    private CommandOption(String shortCommand, String description) {

        this.shortCommand = shortCommand;
        this.description = description;
    }

    public static CommandOption of(String shortCommand, String description) {
        return new CommandOption(shortCommand, description);
    }

    public boolean isPresent() {
        return present || (values != null && !values.isEmpty());
    }

    public boolean isMultipleSubChoices() {
        return multipleSubChoices;
    }

    public Map<String, CommandOptions> getSubChoicesValues() {
        return subChoicesValues;
    }

    public boolean hasSubChoices() {
        return !subChoices.isEmpty();
    }

    @Override
    public String toString() {
        var all = new ArrayList<String>();
        if (shortCommand != null) {
            all.add("shortCommand='" + shortCommand + '\'');
        }
        if (longCommand != null) {
            all.add("longCommand='" + longCommand + '\'');
        }
        return "CommandOption{" +
                String.join(",", all) +
                '}';
    }

    public String getShortCommand() {
        return shortCommand;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHasParameter() {
        return hasParameter;
    }

    public boolean isMandatoryParameter() {
        return mandatoryParameter;
    }

    public String getLongCommand() {
        return longCommand;
    }

    public List<CommandOptions> getSubChoices() {
        return subChoices;
    }

    public CommandOption withParameter() {
        this.hasParameter = true;
        return this;
    }

    public CommandOption withMandatoryParameter() {
        this.mandatoryParameter = true;
        return withParameter();
    }

    public CommandOption withLong(String longCommand) {
        this.longCommand = longCommand;
        return this;
    }

    public CommandOption withSubChoices(CommandOptions... subChoices) {
        for (var commandOption : subChoices) {

            commandOption.setParent(this);
            if (duplicateGuardSubChoices.contains(commandOption.getId().toLowerCase())) {
                throw new RuntimeException("Duplicate sub choice " + commandOption.getId());
            }
            duplicateGuardSubChoices.add(commandOption.getId().toLowerCase());
        }
        this.subChoices.addAll(List.of(subChoices));
        return this;
    }


    public List<String> getLongShortCommandsChild() {
        var result = new ArrayList<String>();
        for (var commandOption : subChoices) {
            result.addAll(commandOption.getLongShortCommands());
        }
        for (var commandOption : subOptions) {
            result.addAll(commandOption.getLongShortCommands());
        }
        return result;
    }

    public List<String> getLongShortCommands() {
        var result = new ArrayList<String>();
        if (shortCommand != null) result.add(shortCommand);
        if (longCommand != null) result.add(longCommand);
        for (var commandOption : subChoices) {
            result.addAll(commandOption.getLongShortCommands());
        }
        return result;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
        if (callback != null) {
            if (!this.isHasParameter()) {
                callback.accept(null);
            } else {
                callback.accept(values.get(0));
            }
        }
    }

    public CommandOption withMultipleSubChoices() {
        this.multipleSubChoices = true;
        return this;
    }

    public void setValues(String value, CommandOptions subChoice) {
        subChoicesValues.put(value.toLowerCase(), subChoice);

    }

    public void setPresent() {
        this.present = true;
    }

    public boolean hasOption(String id) {
        var mainAndChild = id.toLowerCase().split("\\.", 2);
        if (subChoicesValues.containsKey(mainAndChild[0])) {
            if (mainAndChild.length == 1) {
                return true;
            }
            return subChoicesValues.get(mainAndChild[0]).hasOption(mainAndChild[1]);
        }

        if (subOptionsValues.containsKey(mainAndChild[0])) {
            if (mainAndChild.length == 1) {
                return subOptionsValues.get(mainAndChild[0]).isPresent();
            }
            return subOptionsValues.get(mainAndChild[0]).hasOption(mainAndChild[1]);
        }
        return false;
    }

    public List<String> getOptionValues(String id) {
        var mainAndChild = id.toLowerCase().split("\\.", 2);
        if (subChoicesValues.containsKey(mainAndChild[0])) {
            if (mainAndChild.length == 1) {
                return new ArrayList<>();
            }
            return subChoicesValues.get(mainAndChild[0]).getOptionValues(mainAndChild[1]);
        }

        if (subOptionsValues.containsKey(mainAndChild[0])) {
            if (mainAndChild.length == 1) {
                return subOptionsValues.get(mainAndChild[0]).getValues();
            }
            return subOptionsValues.get(mainAndChild[0]).getOptionValues(mainAndChild[1]);
        }
        return new ArrayList<>();
    }

    public CommandOption withCallback(Consumer<String> callback) {
        this.callback = callback;
        return this;
    }

    public String getSubChoicesDescription() {
        return subChoicesDescription;
    }

    public CommandOption withSubChoicesDescription(String subChoicesDescription) {
        this.subChoicesDescription = subChoicesDescription;
        return this;
    }

    public CommandOption getCommandOption(String id) {
        var mainAndChild = id.toLowerCase().split("\\.", 2);
        if (subChoicesValues.containsKey(mainAndChild[0])) {
            if (mainAndChild.length == 1) {
                return null;
            }
            return subChoicesValues.get(mainAndChild[0]).getCommandOption(mainAndChild[1]);
        }
        if (subOptionsValues.containsKey(mainAndChild[0])) {
            if (mainAndChild.length == 1) {
                return subOptionsValues.get(mainAndChild[0]);
            }
            return subOptionsValues.get(mainAndChild[0]).getCommandOption(mainAndChild[1]);
        }

        return null;
    }

    public CommandOption withCommandOptions(CommandOption... subOptionsList) {
        for (var commandOption : subOptionsList) {

            commandOption.setParent(this);
            if (commandOption.getShortCommand() != null && duplicateGuardSubOptions.contains(commandOption.getShortCommand().toLowerCase())) {
                throw new RuntimeException("Duplicate sub option " + commandOption);
            }
            if (commandOption.getLongCommand() != null && duplicateGuardSubOptions.contains(commandOption.getLongCommand().toLowerCase())) {
                throw new RuntimeException("Duplicate sub option " + commandOption);
            }
            if (commandOption.getShortCommand() != null) {
                subOptionsValues.put(commandOption.getShortCommand().toLowerCase(), commandOption);
                duplicateGuardSubOptions.add(commandOption.getShortCommand().toLowerCase());
            }
            if (commandOption.getLongCommand() != null) {
                subOptionsValues.put(commandOption.getLongCommand().toLowerCase(), commandOption);
                duplicateGuardSubOptions.add(commandOption.getLongCommand().toLowerCase());
            }
        }
        this.subOptions.addAll(List.of(subOptionsList));
        return this;
    }

    public boolean hasSubOptions() {
        return !subOptions.isEmpty();
    }

    public void parseInternal(List<MainArg> mainArgs) {
        parseListOfCommands(mainArgs, subOptions, this);
    }

    public List<CommandOption> getCommandOptions() {
        return subOptions;
    }

    public void printHelp(ArrayList<HelpLine> result) {
        printHelpListOfCommands(result, subOptions);
    }

    public CommandItem getParent() {
        return parent;
    }

    public void setParent(CommandItem parent) {
        this.parent = parent;
    }
}
