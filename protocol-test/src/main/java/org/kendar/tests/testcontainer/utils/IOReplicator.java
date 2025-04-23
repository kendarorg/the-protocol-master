package org.kendar.tests.testcontainer.utils;

import java.io.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class IOReplicator {
    private final OutputStream stdin;
    private final Process process;
    private final BufferedWriter writer;

    public IOReplicator(Process process) {
        stdin = process.getOutputStream();
        InputStream stdout = process.getInputStream();
        InputStream stderr = process.getErrorStream();
        this.process = process;
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
        writer = new BufferedWriter(new OutputStreamWriter(stdin));
        BufferedReader error = new BufferedReader(new InputStreamReader(stderr));
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
                result.append(res).append("\n");
                for (var exp : res.split("\n")) {
                    System.out.println("[OUT]\t" + exp);
                }
            }

            if ((len = process.getErrorStream().available()) > 0) {
                byte[] buf = new byte[len];
                process.getErrorStream().read(buf);
                var res = new String(buf).trim();
                result.append(res).append("\n");
                for (var exp : res.split("\n")) {
                    System.err.println("[ERR]\t" + exp);
                }
            }
        } catch (Exception e) {
            result.append("[ERR]\tException ").append(e.getMessage());
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
