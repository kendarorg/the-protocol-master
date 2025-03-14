package org.kendar.mqtt;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;

@TpmService
public class MqttJteResolver extends JteResolver {
    public MqttJteResolver() {

        super(MqttJteResolver.class.getClassLoader());
    }
}
