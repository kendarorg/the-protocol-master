package org.kendar.kafka;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;
import org.pf4j.Extension;

@Extension
@TpmService
public class KafkaJteResolver extends JteResolver {
    public KafkaJteResolver() {
        super(KafkaJteResolver.class.getClassLoader());
    }
}
