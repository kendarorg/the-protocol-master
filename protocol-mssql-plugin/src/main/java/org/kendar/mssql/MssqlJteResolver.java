package org.kendar.mssql;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;
import org.pf4j.Extension;

@Extension
@TpmService
public class MssqlJteResolver extends JteResolver {
    public MssqlJteResolver() {

        super(MssqlJteResolver.class.getClassLoader());
    }
}
