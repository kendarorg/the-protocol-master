package org.kendar.amqp.v10;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;
import org.pf4j.Extension;

@Extension
@TpmService
public class Amqp10JteResolver extends JteResolver {
    public Amqp10JteResolver() {
        super(Amqp10JteResolver.class.getClassLoader());
    }
}
