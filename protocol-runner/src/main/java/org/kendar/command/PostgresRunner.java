package org.kendar.command;

import org.kendar.annotations.TpmService;

@TpmService
public class PostgresRunner extends JdbcRunner{
    public PostgresRunner() {
        super("postgres");
    }
}
