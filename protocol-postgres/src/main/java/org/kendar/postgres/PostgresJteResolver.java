package org.kendar.postgres;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;

@TpmService
public class PostgresJteResolver extends JteResolver {
    public PostgresJteResolver() {

        super(PostgresJteResolver.class.getClassLoader());
    }
}
