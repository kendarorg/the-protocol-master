package org.kendar.mysql;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;

@TpmService
public class MySQLJteResolver extends JteResolver {
    public MySQLJteResolver() {

        super(MySQLJteResolver.class.getClassLoader());
    }
}
