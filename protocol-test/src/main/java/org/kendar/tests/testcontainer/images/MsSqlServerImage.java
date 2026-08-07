package org.kendar.tests.testcontainer.images;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.kendar.tests.testcontainer.utils.BaseImage;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"SqlSourceToSinkFlow", "rawtypes", "resource"})
public class MsSqlServerImage extends BaseImage<MsSqlServerImage, MSSQLServerContainer> {
    private final List<ScriptList> initScripts = new ArrayList<>();
    private String jdbcUrl;
    private String userId;
    private String password;

    public MsSqlServerImage() {
        this.withExposedPorts(1433);
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

    public MsSqlServerImage withInitScript(String db, String initScriptPath) {
        try {
            if (db == null) throw new RuntimeException();
            ScriptList ls = new ScriptList();
            ls.db = db;
            ls.scriptPath = initScriptPath;
            Path path = Path.of(initScriptPath);
            if (Files.exists(path)) {
                ls.scriptContent = String.join("\n", Files.readAllLines(path));
            } else {
                InputStream stream = MsSqlServerImage.class.getResourceAsStream(initScriptPath);
                if (null != stream) {
                    ls.scriptContent = new String(stream.readAllBytes());
                    stream.close();
                } else {
                    stream = MsSqlServerImage.class.getResourceAsStream("/" + initScriptPath);
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
        container = new MSSQLServerContainer<>(
                DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
                .acceptLicense();
    }

    @Override
    protected void postStart() {
        jdbcUrl = container.getJdbcUrl() + ";encrypt=false;trustServerCertificate=true";
        userId = container.getUsername();
        password = container.getPassword();
        String lastScript = "";
        try {
            for (ScriptList st : initScripts) {
                lastScript = st.scriptPath;
                Connection connection = DriverManager.getConnection(jdbcUrl, userId, password);
                connection.createStatement().execute(
                        "IF DB_ID('" + st.db + "') IS NULL CREATE DATABASE " + st.db);
                connection.createStatement().execute("USE " + st.db);
                ScriptRunner scriptRunner = new ScriptRunner(connection);
                scriptRunner.setSendFullScript(false);
                scriptRunner.setStopOnError(true);
                scriptRunner.runScript(new StringReader(st.scriptContent));
                connection.close();
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
