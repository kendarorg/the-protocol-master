package org.kendar.tests.testcontainer.images;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.kendar.tests.testcontainer.utils.BaseImage;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("rawtypes")
public class PostgresSqlImage extends BaseImage<PostgresSqlImage, PostgreSQLContainer> {
    private final List<ScriptList> initScripts = new ArrayList<>();
    private String dbName;
    private String jdbcUrl;
    private String userId;
    private String password;

    public PostgresSqlImage() {
        this.withExposedPorts(5432);
    }

    public String getDbName() {
        return dbName;
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

    public PostgresSqlImage withInitScript(String db, String initScriptPath) {
        try {
            if (db == null) throw new RuntimeException();
            ScriptList ls = new ScriptList();
            ls.db = db;
            ls.scriptPath = initScriptPath;
            Path path = Path.of(initScriptPath);
            if (Files.exists(path)) {
                ls.scriptContent = String.join("\n", Files.readAllLines(path));
            } else {
                InputStream stream = PostgresSqlImage.class.getResourceAsStream(initScriptPath);
                if (null != stream) {
                    ls.scriptContent = new String(stream.readAllBytes());
                    stream.close();
                } else {
                    stream = PostgresSqlImage.class.getResourceAsStream("/" + initScriptPath);
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

        container = new PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"));
        container.setEnv(Arrays.asList("POSTGRES_USER=root", "POSTGRES_PASSWORD=test"));
    }

    @Override
    protected void postStart() {
        jdbcUrl = container.getJdbcUrl();
        userId = container.getUsername();
        password = container.getPassword();
        dbName = container.getDatabaseName();
        String lastScript = "";
        try {
            for (ScriptList st : initScripts) {
                lastScript = st.scriptPath;
                Connection connection = DriverManager.getConnection(jdbcUrl, userId, password);
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
