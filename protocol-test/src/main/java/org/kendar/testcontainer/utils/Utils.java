package org.kendar.testcontainer.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Pattern DOCKER_HOST_PATTERN = Pattern.compile("(.+)://([^:]+)(.*)");

    public static String getJarName(String jarName, String... projectPath) {
        Path rootPath = getRootPath();
        String fullProjectPath = String.join(File.pathSeparator, projectPath);
        Path jarPath = Path.of(rootPath.toString(), fullProjectPath, "target");
        String[] flist = jarPath.toFile().list();
        if (flist == null) {
            throw new RuntimeException();
        }
        for (String file : flist) {
            if (file.startsWith(jarName) && file.endsWith(".jar")) {
                return file;
            }
        }
        return null;
    }

    @SuppressWarnings("CatchMayIgnoreException")
    public static String getDockerHost() {
        try {

            Matcher matcher = DOCKER_HOST_PATTERN.matcher(System.getenv("DOCKER_HOST"));
            if (matcher.matches()) {
                return matcher.group(2);
            }
        } catch (Exception ex) {

        }
        return null;
    }

    @SuppressWarnings("DataFlowIssue")
    public static Path getRootPath() {
        ClassLoader classLoader = Utils.class.getClassLoader();
        var ff = classLoader.getResource("marker.mk").getFile();
        File file = new File(ff);
        Path absolutePath = Path.of(file.getAbsolutePath());

        String res = absolutePath.toString();
        int ht = res.indexOf("ham-test");
        return Path.of(res.substring(0, ht - 1));
    }

    public static String simpleGetRequest(String address) throws Exception {


        URL url = new URL(address);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return content.toString();
    }
}
