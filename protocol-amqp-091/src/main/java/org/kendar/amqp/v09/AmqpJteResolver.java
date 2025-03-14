package org.kendar.amqp.v09;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;

@TpmService
public class AmqpJteResolver extends JteResolver {
    public AmqpJteResolver() {

        super(AmqpJteResolver.class.getClassLoader());
    }
}
