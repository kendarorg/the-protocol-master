package org.kendar.mssql;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;

@TpmService
public class MssqlJteResolver extends JteResolver {
    public MssqlJteResolver() {

        super(MssqlJteResolver.class.getClassLoader());
    }
}
