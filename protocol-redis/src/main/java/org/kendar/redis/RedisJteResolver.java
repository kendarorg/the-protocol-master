package org.kendar.redis;

import org.kendar.di.annotations.TpmService;
import org.kendar.ui.JteResolver;

@TpmService
public class RedisJteResolver extends JteResolver {
    public RedisJteResolver() {

        super(RedisJteResolver.class.getClassLoader());
    }
}
