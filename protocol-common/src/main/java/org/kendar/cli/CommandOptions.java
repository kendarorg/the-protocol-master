package org.kendar.cli;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommandOptions implements CommandItem {
    private final String id;
    private final List<CommandOption> commandOptions = new ArrayList<>();
    private final Set<String> duplicateGuard = new HashSet<>();
    private Consumer<String> callback;
    private String description;
    private CommandItem parent;

    private CommandOptions(String id, String description) {

        this.id = id;
        this.description = description;
    }

    public static CommandOptions of(String id) {
        return new CommandOptions(id, null);
    }

    public static CommandOptions of(String id, String description) {
        return new CommandOptions(id, description);
    }

    protected static void parseListOfCommands(List<MainArg> mainArgs, List<CommandOption> co, CommandItem caller) {
        for (CommandOption item : co) {
            var matchingArgIndex = 0;
            var foundedMatchingArg = false;
            for (; matchingArgIndex < mainArgs.size(); matchingArgIndex++) {
                var mainArg = mainArgs.get(matchingArgIndex);
                if (mainArg.getId().equalsIgnoreCase(item.getLongCommand()) || mainArg.getId().equalsIgnoreCase(item.getShortCommand())) {
                    foundedMatchingArg = true;
                    break;
                }
            }
            if (!foundedMatchingArg) {
                continue;
            }
            var parent = caller.getParent();
            if (parent != null) {
                var isMine = true;
                var commandOptions = parent.getCommandOptions();
                for (CommandOption coFounded : commandOptions) {
                    for (var arg = 0; arg < matchingArgIndex; arg++) {
                        var wrongArg = mainArgs.get(arg);
                        if (wrongArg.getId().equalsIgnoreCase(coFounded.getLongCommand()) || wrongArg.getId().equalsIgnoreCase(coFounded.getShortCommand())) {
                            isMine = false;
                            break;
                        }
                    }

                }
                if (!isMine) {
                    continue;
                }
            }


            /*var firstDifferent =-1;
            for(int z=i+1;z<co.size();z++) {
                var tz = co.get(z);
                if()
                if(co.)
            }
            for (int j = 0; j < mainArgs.size(); j++) {
                var arg = mainArgs.get(j);


            }*/
            var arg = mainArgs.stream().filter(a -> a.getId().equalsIgnoreCase(item.getShortCommand()) || a.getId().equalsIgnoreCase(item.getLongCommand())).findFirst();
            if (arg.isEmpty()) {
                continue;
            }


            if (!item.hasSubChoices()) {
                mainArgs.remove(arg.get());
                if (item.isMandatoryParameter() && arg.get().getValues().isEmpty()) {
                    throw new RuntimeException("Mandatory parameter " + item + " not present");
                }
                item.setValues(arg.get().getValues());
                item.setPresent();
                if (item.hasSubOptions()) {
                    item.parseInternal(mainArgs);
                }
            } else {
                while (true) {
                    var choices = item.getSubChoices().stream().map(CommandOptions::getId).collect(Collectors.toCollection(HashSet::new));
                    var availableChoices = String.join(", ", choices);
                    if (arg.get().getValues().isEmpty()) {
                        throw new RuntimeException("Missing value " + item + " in command option " + item + " available choices are " + availableChoices);
                    }
                    if (arg.get().getValues().size() > 1) {
                        throw new RuntimeException("Duplicate value " + item + " in command option " + item + " available choices are " + availableChoices);
                    }
                    var value = arg.get().getValues().get(0);
                    if (!choices.contains(value)) {
                        throw new RuntimeException("Wrong value " + value + " in command option " + item + " available choices are " + availableChoices);
                    }
                    mainArgs.remove(arg.get());
                    var itemPossible = item.getSubChoices().stream().filter(sc -> sc.getId().equalsIgnoreCase(value)).findFirst();
                    if (itemPossible.isEmpty()) {
                        throw new RuntimeException("Wrong option " + value + " in command option " + item + " available choices are " + availableChoices);
                    }
                    var subChoice = itemPossible.get();
                    if (subChoice.callback != null) {
                        subChoice.callback.accept(value);
                    }
                    subChoice.parseInternal(mainArgs);
                    item.setValues(value, subChoice);

                    if (!item.isMultipleSubChoices()) {
                        break;
                    } else {
                        arg = mainArgs.stream().filter(a -> a.getId().equalsIgnoreCase(item.getShortCommand()) || a.getId().equalsIgnoreCase(item.getLongCommand())).findFirst();
                        if (arg.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }
    }

    protected static void printHelpListOfCommands(ArrayList<HelpLine> result, List<CommandOption> co) {
        for (var item : co) {
            if (item.hasSubChoices()) continue;
            result.add(new HelpLine(item.getShortCommand(), item.getLongCommand(), item.getDescription(), null));
            if (item.hasSubOptions()) {
                for (var choice : item.getCommandOptions()) {
                    if (choice.getDescription() != null) {
                        choice.printHelp(result);
                    }
                }
            }
        }
        for (var item : co) {
            if (!item.hasSubChoices()) continue;
            var choices = item.getSubChoices().stream().map(CommandOptions::getId).collect(Collectors.toCollection(HashSet::new));
            var availableChoices = String.join("|", choices);
            result.add(new HelpLine(item.getShortCommand(), item.getLongCommand(), item.getDescription(), availableChoices));
            if (item.getSubChoicesDescription() != null) {
                result.add(new HelpLine(item.getSubChoicesDescription()));
            }
            for (var choice : item.getSubChoices()) {
                if (choice.getDescription() != null) {
                    result.add(new HelpLine(choice.getDescription()));
                    choice.printHelp(result);
                }
            }
        }
    }

    public List<CommandOption> getCommandOptions() {
        return commandOptions;
    }

    public String getId() {
        return id;
    }

    public CommandOptions withOptions(CommandOption... commandOptions) {

        for (var commandOption : commandOptions) {
            commandOption.setParent(this);
            if (commandOption.hasSubChoices()) {
                if (commandOption.getLongCommand() != null) {
                    if (duplicateGuard.contains(commandOption.getLongCommand())) {
                        throw new RuntimeException("Duplicate command " + commandOption + " " + commandOption.getLongCommand());
                    }
                    duplicateGuard.add(commandOption.getLongCommand());
                }
                if (commandOption.getShortCommand() != null) {
                    if (duplicateGuard.contains(commandOption.getShortCommand())) {
                        throw new RuntimeException("Duplicate command " + commandOption + " " + commandOption.getShortCommand());
                    }
                    duplicateGuard.add(commandOption.getShortCommand());
                }
                continue;
            }
            for (var item : commandOption.getLongShortCommands()) {
                if (duplicateGuard.contains(item.toLowerCase())) {
                    throw new RuntimeException("Duplicate inherited command " + item + " on option " + commandOption);
                }
                duplicateGuard.add(item.toLowerCase());
            }
        }
        for (var commandOption : commandOptions) {
            if (commandOption.hasSubChoices() || commandOption.hasSubOptions()) {
                for (var item : commandOption.getLongShortCommandsChild()) {
                    if (duplicateGuard.contains(item.toLowerCase())) {
                        throw new RuntimeException("Duplicate inherited command " + item + " on option " + commandOption);
                    }
                }
            }
        }

        this.commandOptions.addAll(List.of(commandOptions));
        return this;
    }

    public List<String> getLongShortCommands() {
        var result = new ArrayList<String>();
        for (var item : commandOptions) {
            result.addAll(item.getLongShortCommands());
        }
        return result;
    }

    public List<String> getLongShortCommandsChild() {
        var result = new ArrayList<String>();
        for (var item : commandOptions) {
            result.addAll(item.getLongShortCommandsChild());
        }
        return result;
    }

    public void parse(List<MainArg> mainArgs, boolean ignoreMissing) {
        parseInternal(mainArgs);
        if (!ignoreMissing && !mainArgs.isEmpty()) {
            throw new RuntimeException("Unknown options " + mainArgs);
        }
    }

    public void parseInternal(List<MainArg> mainArgs) {
        parseListOfCommands(mainArgs, commandOptions, this);
    }

    public boolean hasOption(String id) {
        var mainAndChild = id.toLowerCase().split("\\.", 2);
        for (var item : commandOptions) {
            if ((item.getShortCommand() != null && item.getShortCommand().equalsIgnoreCase(mainAndChild[0])) ||
                    (item.getLongCommand() != null && item.getLongCommand().equalsIgnoreCase(mainAndChild[0]))) {
                if (item.hasSubChoices()) {
                    if (mainAndChild.length == 1) {
                        return !item.getSubChoicesValues().isEmpty();
                    } else if (item.hasOption(mainAndChild[1])) {
                        return true;
                    }
                } else if (item.isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getOptionValues(String id) {
        var mainAndChild = id.toLowerCase().split("\\.", 2);
        for (var item : commandOptions) {
            if ((item.getShortCommand() != null && item.getShortCommand().equalsIgnoreCase(mainAndChild[0])) ||
                    (item.getLongCommand() != null && item.getLongCommand().equalsIgnoreCase(mainAndChild[0]))) {
                if (item.hasSubOptions()) {
                    if (mainAndChild.length > 1 && item.hasOption(mainAndChild[1])) {
                        return item.getOptionValues(mainAndChild[1]);
                    }
                }
                if (item.hasSubChoices()) {
                    if (mainAndChild.length == 1) {
                        return new ArrayList<>(item.getSubChoicesValues().keySet());
                    } else if (item.hasOption(mainAndChild[1])) {
                        return item.getOptionValues(mainAndChild[1]);
                    }
                } else if (item.isPresent()) {
                    return item.getValues();
                }
            }
        }
        return null;
    }

    public CommandOptions withCallback(Consumer<String> callback) {
        this.callback = callback;
        return this;
    }

    public void printHelp(ArrayList<HelpLine> result) {
        printHelpListOfCommands(result, commandOptions);
    }

    public String getDescription() {
        return description;
    }

    public CommandOptions withDescription(String description) {
        this.description = description;
        return this;
    }

    public CommandOption getCommandOption(String id) {
        var mainAndChild = id.toLowerCase().split("\\.", 2);
        for (var item : commandOptions) {
            if (mainAndChild[0].equalsIgnoreCase(item.getShortCommand()) || mainAndChild[0].equalsIgnoreCase(item.getLongCommand())) {
                if (!item.hasSubChoices() || mainAndChild.length == 1) {
                    return item;
                } else {
                    var result = item.getCommandOption(mainAndChild[1]);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    public CommandItem getParent() {
        return parent;
    }

    public void setParent(CommandItem parent) {
        this.parent = parent;
    }
}
