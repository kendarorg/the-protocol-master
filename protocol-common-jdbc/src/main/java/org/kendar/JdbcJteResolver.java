package org.kendar;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;
import org.pf4j.Extension;

@Extension
@TpmService
public class JdbcJteResolver extends JteResolver {
    public JdbcJteResolver() {

        super(JdbcJteResolver.class.getClassLoader());
    }
}
