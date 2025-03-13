package org.kendar.ui;

import gg.jte.CodeResolver;
import gg.jte.TemplateNotFoundException;
import org.kendar.di.annotations.TpmService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@TpmService
public class MultiCodeResolver implements CodeResolver {
    private final List<JteResolver> jteResolvers;
    private final ConcurrentHashMap<String, CodeResolver> engines= new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,Object> notEngines= new ConcurrentHashMap<>();

    public MultiCodeResolver(List<JteResolver> resolvers) {
        this.jteResolvers = resolvers;
    }

    @Override
    public String resolve(String name) {
        try {
            return resolveRequired(name);
        } catch (TemplateNotFoundException e) {
            return null;
        }
    }

    @Override
    public String resolveRequired(String name) throws TemplateNotFoundException {
        if(engines.containsKey(name)) {
            return engines.get(name).resolve(name);
        }else if(notEngines.containsKey(name)) {
            throw new TemplateNotFoundException(name + " not found");
        }
        String resolved = null;
        for(var jteResolver : jteResolvers) {
            var resolver = jteResolver.getResolver();
            resolved = resolver.resolve(name);
            if(resolved != null) {
                engines.put(name, resolver);
                return resolved;
            }
        }
        if(resolved == null) {
            notEngines.put(name, name);
            throw new TemplateNotFoundException(name + " not found");
        }
        return resolved;
    }

    @Override
    public long getLastModified(String s) {
        return 0;
    }

    @Override
    public List<String> resolveAllTemplateNames() {
        throw new UnsupportedOperationException("This code resolver does not support finding all template names!");
    }

    @Override
    public boolean exists(String name) {
        return this.resolve(name) != null;
    }
}
