package org.kendar.testcontainer.images;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.kendar.testcontainer.utils.BaseImage;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"SqlSourceToSinkFlow", "rawtypes"})
public class MysqlImage extends BaseImage<MysqlImage, MySQLContainer> {
    private final List<ScriptList> initScripts = new ArrayList<>();
    private String jdbcUrl;
    private String userId;
    private String password;

    public MysqlImage() {
        this.withExposedPorts(3306);
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public MysqlImage withInitScript(String db, String initScriptPath) {
        try {
            if (db == null) throw new RuntimeException();
            ScriptList ls = new ScriptList();
            ls.db = db;
            ls.scriptPath = initScriptPath;
            Path path = Path.of(initScriptPath);
            if (Files.exists(path)) {
                ls.scriptContent = String.join("\n", Files.readAllLines(path));
            } else {
                InputStream stream = MysqlImage.class.getResourceAsStream(initScriptPath);
                if (null != stream) {
                    ls.scriptContent = new String(stream.readAllBytes());
                    stream.close();
                } else {
                    stream = MysqlImage.class.getResourceAsStream("/" + initScriptPath);
                    if (null != stream) {
                        ls.scriptContent = new String(stream.readAllBytes());
                        stream.close();
                    }
                }
            }
            if (ls.scriptContent == null) {
                throw new RuntimeException();
            }
            this.initScripts.add(ls);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return this;
    }

    @Override
    protected void preStart() {

        container = new MySQLContainer<>(DockerImageName.parse("mysql:8"));
        container.withTmpFs(Map.of("/var/lib/mysql", "rw"));
        container.setEnv(List.of("MYSQL_ROOT_PASSWORD=test"));
    }

    @Override
    protected void postStart() {
        jdbcUrl = container.getJdbcUrl();
        userId = "root";
        password = container.getPassword();
        String lastScript = "";
        try {
            for (ScriptList st : initScripts) {
                lastScript = st.scriptPath;
                Connection connection = DriverManager.getConnection(jdbcUrl, userId, password);
                connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS " + st.db);
                connection.createStatement().execute("GRANT ALL ON *.* TO '" + userId + "'@'%'");
                ScriptRunner scriptRunner = new ScriptRunner(connection);
                scriptRunner.setSendFullScript(false);
                scriptRunner.setStopOnError(true);
                scriptRunner.runScript(new StringReader(st.scriptContent));
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error executing " + lastScript, ex);
        }
    }

    static class ScriptList {
        String db;
        String scriptContent;
        String scriptPath;
    }
}
