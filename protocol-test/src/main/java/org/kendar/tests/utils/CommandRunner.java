package org.kendar.tests.utils;

import java.nio.file.Path;

@SuppressWarnings("ThrowablePrintedToSystemOut")
public class CommandRunner {
    private final String path;
    private final String[] commands;

    public CommandRunner(String path, String... commands) {
        this.path = path;
        this.commands = commands;
    }

    public CommandRunner(Path path, String... commands) {
        this.path = path.toString();
        this.commands = commands;
    }

    public void run() {
        try {
            var pb = new ProcessBuilder();
            pb.directory(Path.of(path).toFile());
            pb.command(commands);
            var process = pb.start();
            System.out.println("Running " + String.join(" ", commands));
            //                    outConsumer.accept(a, process);
            //                    if (maxLines.get() <= 0) return;
            //                    maxLines.decrementAndGet();
            //LogWriter.writeProcess(a);
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(),
                    System.out::println);
            //LogWriter.writeProcess(a);
            //errorConsumer.accept(a, process);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(),
                    System.err::println);

            new Thread(outputGobbler).start();
            new Thread(errorGobbler).start();
            process.waitFor();
            System.out.println("Completed " + String.join(" ", commands));
        } catch (Exception e) {
            System.err.println("Error running " + String.join(" ", commands));
            System.err.println(e);
        }
    }
}
