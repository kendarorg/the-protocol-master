package org.kendar.cli;

import java.util.ArrayList;
import java.util.List;

public class CommandParser {
    private final CommandOptions options;
    private List<MainArg> mainArgs;
    private boolean printedHelp = false;


    public CommandParser(CommandOptions options) {
        this.options = options;
    }

    public CommandOptions getOptions() {
        return options;
    }

    public List<MainArg> getMainArgs() {
        return mainArgs;
    }

    private void buildArgs(String[] args) {
        mainArgs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            if (arg.startsWith("-")) {
                var name = arg.substring(1);
                var mainArg = new MainArg(name.toLowerCase());
                mainArgs.add(mainArg);
                i++;
                for (; i < args.length; i++) {
                    var arg2 = args[i];
                    if (arg2.startsWith("-")) {
                        i--;
                        break;
                    } else {
                        mainArg.addValue(arg2);
                    }
                }
            }
        }
    }

    public void parse(String[] args) {

        try {
            buildArgs(args);
            options.parse(mainArgs, false);
            if (!mainArgs.isEmpty()) {
                printHelp();
            }
        } finally {
            if (!mainArgs.isEmpty()) {
                printHelp();
            }
        }
    }

    public void parseIgnoreMissing(String[] args) {

        buildArgs(args);
        options.parse(mainArgs, true);

    }

    public boolean hasOption(String id) {
        return options.hasOption(id);
    }

    public String getOptionValue(String id) {
        return getOptionValue(id, null);
    }

    public String getOptionValue(String id, String defaultValue) {
        var v = options.getOptionValues(id);
        if (v == null || v.isEmpty()) {
            return defaultValue;
        }
        return v.get(0);
    }

    public List<String> getOptionValues(String id, String... defaultValues) {
        var v = options.getOptionValues(id);
        if (v == null || v.isEmpty()) {
            return new ArrayList<>(List.of(defaultValues));
        }
        return v;
    }

    public void printHelp() {
        if (printedHelp) return;
        printedHelp = true;
        var toPrint = buildHelp();
        System.out.println(toPrint);
    }

    public String buildHelp() {
        var result = new ArrayList<HelpLine>();
        var toPrint = new StringBuilder();

        if (options.getDescription() != null) result.add(new HelpLine(options.getDescription()));
        options.printHelp(result);
        var maxShort = 0;
        var maxLong = 0;
        for (var item : result) {
            if (item.getShortCommand() != null) {
                maxShort = Math.max(maxShort, item.getShortCommand().length());
            } else {
                item.setShortCommand("");
            }
            if (item.getLongCommand() != null) {
                maxLong = Math.max(maxLong, item.getLongCommand().length());
            } else {
                item.setLongCommand("");
            }
            if (item.getDescription() == null) {
                item.setDescription("");
            }
        }
        var sbShort = new StringBuilder();
        for (int i = 0; i < maxShort; i++) {
            sbShort.append(' ');
        }
        var sbLong = new StringBuilder();
        for (int i = 0; i < maxLong; i++) {
            sbLong.append(' ');
        }
        var max = 130 - maxLong - maxShort - 3;
        var firstLine = true;
        for (var item : result) {
            if (!item.isBlock()) {
                item.setShortCommand(item.getShortCommand() + sbShort.substring(item.getShortCommand().length()));
                item.setLongCommand(item.getLongCommand() + sbLong.substring(item.getLongCommand().length()));
                var description = item.getDescription();
                if (item.getAvailableOptions() != null) {
                    description += "\nOptions: " + item.getAvailableOptions();
                }
                String[] split = description.split("[\r\n\f]+");
                for (int i = 0; i < split.length; i++) {
                    var descline = split[i];
                    if (i == 0) {
                        toPrint.append("\t" + item.getShortCommand() + "\t" + item.getLongCommand() + "\t" + descline + "\n");
                    } else {
                        toPrint.append("\t" + sbShort + "\t" + sbLong + "\t" + descline + "\n");
                    }
                }
            } else {
                if (!firstLine) {
                    toPrint.append("\n");
                }
                toPrint.append(item.getDescription() + "\n");
                toPrint.append("\n");
                firstLine = false;
            }
        }
        return toPrint.toString();
    }
}