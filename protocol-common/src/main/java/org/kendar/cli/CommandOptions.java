package org.kendar.cli;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommandOptions {
    private final String id;
    private final List<CommandOption> commandOptions = new ArrayList<>();
    private final Set<String> duplicateGuard = new HashSet<>();
    private Consumer<String> callback;
    private String description;

    private CommandOptions(String id, String description) {

        this.id = id;
        this.description = description;
    }

    public static CommandOptions of(String id) {
        return new CommandOptions(id,null);
    }

    public static CommandOptions of(String id, String description) {
        return new CommandOptions(id,description);
    }

    public List<CommandOption> getCommandOptions() {
        return commandOptions;
    }

    public String getId() {
        return id;
    }

    public CommandOptions withOptions(CommandOption... commandOptions) {

        for (var commandOption : commandOptions) {
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
            if (!commandOption.hasSubChoices()) continue;
            for (var item : commandOption.getLongShortCommandsChild()) {
                if (duplicateGuard.contains(item.toLowerCase())) {
                    throw new RuntimeException("Duplicate inherited command " + item + " on option " + commandOption);
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

    public void parse(List<MainArg> mainArgs) {
        parseInternal(mainArgs);
        if (!mainArgs.isEmpty()) {
            throw new RuntimeException("Unknown options " + mainArgs);
        }
    }

    public void parseInternal(List<MainArg> mainArgs) {
        for (var item : commandOptions) {
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
                    if(itemPossible.isEmpty()){
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
                    } else {
                        if (item.isPresent()) {
                            return true;
                        }
                        break;
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
            if (item.getShortCommand().equalsIgnoreCase(mainAndChild[0]) ||
                    item.getLongCommand().equalsIgnoreCase(mainAndChild[0])) {
                if (item.hasSubChoices()) {
                    if (mainAndChild.length == 1) {
                        return new ArrayList<>(item.getSubChoicesValues().keySet());
                    } else if (item.hasOption(mainAndChild[1])) {
                        return item.getOptionValues(mainAndChild[1]);
                    } else {
                        if (item.isPresent()) {
                            return item.getValues();
                        }
                        break;
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
        for(var item:commandOptions){
            if(item.hasSubChoices())continue;
            result.add(new HelpLine(item.getShortCommand(), item.getLongCommand(),item.getDescription(),null));
        }
        for(var item:commandOptions){
            if(!item.hasSubChoices())continue;
            var choices = item.getSubChoices().stream().map(CommandOptions::getId).collect(Collectors.toCollection(HashSet::new));
            var availableChoices = String.join("|", choices);
            result.add(new HelpLine(item.getShortCommand(), item.getLongCommand(),item.getDescription(),availableChoices));
            if(item.getSubChoicesDescription()!=null){
                result.add(new HelpLine(item.getSubChoicesDescription()));
            }
            for(var choice:item.getSubChoices()){
                if(choice.getDescription()!=null){
                    result.add(new HelpLine(choice.getDescription()));
                    choice.printHelp(result);
                }
            }
        }
    }

    public String getDescription() {
        return description;
    }

    public CommandOptions withDescription(String description) {
        this.description = description;
        return this;
    }
}
