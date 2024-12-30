package org.kendar.command;

import org.kendar.annotations.di.TpmService;

@TpmService
public class MySQLRunner extends JdbcRunner{
    public MySQLRunner() {
        super("mysql");
    }
}
