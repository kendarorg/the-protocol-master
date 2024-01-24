package org.kendar.testcontainer.utils;

import java.io.*;

public class IOReplicator {
    private final OutputStream stdin;
    private final InputStream stdout;
    private final InputStream stderr;
    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final BufferedReader error;

    public IOReplicator(Process process) {
        stdin = process.getOutputStream();
        stdout = process.getInputStream();
        stderr = process.getErrorStream();
        this.process = process;
        reader = new BufferedReader(new InputStreamReader(stdout));
        writer = new BufferedWriter(new OutputStreamWriter(stdin));
        error = new BufferedReader(new InputStreamReader(stderr));
    }

    public String showData() {
        return showData(1000);
    }

    public String showData(int ms) {

        var result = new StringBuilder();
        try {
            Thread.sleep(ms);
            int len = 0;
            if ((len = process.getInputStream().available()) > 0) {
                byte[] buf = new byte[len];
                process.getInputStream().read(buf);
                var res = new String(buf).trim();
                result.append(res + "\n");
                for (var exp : res.split("\n")) {
                    System.out.println("[OUT]\t" + exp);
                }
            }

            if ((len = process.getErrorStream().available()) > 0) {
                byte[] buf = new byte[len];
                process.getErrorStream().read(buf);
                var res = new String(buf).trim();
                result.append(res + "\n");
                for (var exp : res.split("\n")) {
                    System.err.println("[ERR]\t" + exp);
                }
            }
        } catch (Exception e) {
            result.append("[ERR]\tException " + e.getMessage());
        }
        return result.toString();
    }

    public String write(String s) {

        try {
            writer.write(s + "\n");
            writer.flush();
            stdin.flush();
            return showData();
        } catch (Exception e) {
            return "[ERR]\tException " + e.getMessage();
        }
    }
}
