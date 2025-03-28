package org.kendar.dns;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;
import org.pf4j.Extension;

@Extension
@TpmService
public class DnsJteResolver extends JteResolver {
    public DnsJteResolver() {

        super(DnsJteResolver.class.getClassLoader());
    }
}
