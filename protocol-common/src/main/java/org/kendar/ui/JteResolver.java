package org.kendar.ui;

import gg.jte.resolve.ResourceCodeResolver;

public class JteResolver implements JteResolverPlugin {

    private final ResourceCodeResolver resolver;

    public JteResolver(ClassLoader classLoader) {
        this.resolver = new ResourceCodeResolver("jte", classLoader);
    }

    public ResourceCodeResolver getResolver() {
        return resolver;
    }
}
