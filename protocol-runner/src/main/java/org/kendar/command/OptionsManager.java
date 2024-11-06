package org.kendar.command;

import org.apache.commons.cli.*;
import org.kendar.filters.FilterDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.ini.Ini;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class OptionsManager {
    private Map<String,CommonProtocol> protocols = new HashMap<>();
    public OptionsManager(CommonProtocol...input){
        for (CommonProtocol protocol : input) {
            protocols.put(protocol.getId(),protocol);
        }
    }
    private static Options getMainOptions() {
        Options options = new Options();
        options.addOption("cfg", true, "Load config file");
        options.addOption("pluginsDir", true, "Plugins directory");
        options.addOption("datadir", true, "Data directory/connection string");
        options.addOption("loglevel", true, "Log4j log level");
        options.addOption("logType", true, "The log type: default [none|file]");
        options.addOption("protocol", true, "Protocol (http|mqtt|amqp091|mysql|postgres|redis|mongo");
        options.addOption(Option.builder().option("help").optionalArg(true).desc("Show contestual help").build());
        return options;
    }

    public Ini run(String[] args) {
        var options = getMainOptions();
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args,true);
            var isExecute = false;
            if (cmd.hasOption("cfg")) {
                var ini = new Ini();
                var configFile = cmd.getOptionValue("cfg");
                ini.load(Path.of(configFile).toAbsolutePath().toFile());
                return ini;
            }else if (cmd.hasOption("help")) {
                var helpValue = cmd.getOptionValue("help");
                checkOptions(helpValue);
                runWithParams(args, helpValue, isExecute,null,options);
                throw new Exception();
            }else{
                isExecute = true;

                var datadir = cmd.getOptionValue("datadir");
                var pluginsDir = cmd.getOptionValue("pluginsDir","plugins");
                var protocol = cmd.getOptionValue("protocol");
                var loglevel = cmd.getOptionValue("loglevel","ERROR");
                var logType = cmd.getOptionValue("logType","file");
                checkOptions(datadir,pluginsDir,protocol);
                var ini = new Ini();
                ini.putValue("global","datadir",datadir);
                ini.putValue("global","pluginsDir",pluginsDir);
                ini.putValue("global","loglevel",loglevel);
                ini.putValue("global","logType",logType);

                ini.putValue(protocol,"protocol",protocol);
                runWithParams(args,protocol, isExecute,ini, options);
                return ini;
            }
        } catch (Exception ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("runner", options);
        }
        return null;
    }

    private void checkOptions(String ... args) throws Exception {
        for(var arg:args){
            if(arg==null){
                throw new Exception();
            }
        }
    }

    private void runWithParams(String[] args, String protocol, boolean isExecute, Ini go, Options options) throws Exception {
        var founded = protocols.get(protocol);
        founded.run(args, isExecute,go,options);
    }

    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, Ini ini, String protocol, StorageRepository storage, ArrayList<FilterDescriptor> filters, Supplier<Boolean> stopWhenFalse) throws Exception {
        var pr = protocols.get(protocol);
        pr.start(protocolServer,key,ini,protocol,storage,filters,stopWhenFalse);
    }
}
