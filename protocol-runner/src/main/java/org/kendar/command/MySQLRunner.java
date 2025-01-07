package org.kendar.command;

import org.kendar.di.annotations.TpmService;

@TpmService
public class MySQLRunner extends JdbcRunner{
    public MySQLRunner() {
        super("mysql");
    }
}
