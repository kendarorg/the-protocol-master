package org.kendar.http;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;

@TpmService
public class HttpJteResolver extends JteResolver {
    public HttpJteResolver() {

        super(HttpJteResolver.class.getClassLoader());
    }
}
