package org.kendar.ui;

import gg.jte.CodeResolver;
import gg.jte.TemplateNotFoundException;
import org.kendar.di.annotations.TpmService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom templates resolver to allow JTE finding the templates into included plugins
 */
@TpmService
public class MultiCodeResolver implements CodeResolver {
    private final List<JteResolver> jteResolvers;
    private final ConcurrentHashMap<String, CodeResolver> engines = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> notEngines = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> resolvedCache = new ConcurrentHashMap<>();

    /**
     * The JTEResolvers are simply a storage for the plugins classloaders
     *
     * @param resolvers
     */
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
        if (resolvedCache.containsKey(name)) {
            return resolvedCache.get(name);
        }
        // If found already who contains the template use it
        if (engines.containsKey(name)) {
            var result = engines.get(name).resolve(name);
            resolvedCache.put(name, result);
            return result;
        } else if (notEngines.containsKey(name)) {
            //Error when not finding
            throw new TemplateNotFoundException(name + " not found");
        }
        String resolved = null;
        for (var jteResolver : jteResolvers) {
            var resolver = jteResolver.getResolver();
            resolved = resolver.resolve(name);
            if (resolved != null) {
                resolvedCache.put(name, resolved);
                engines.put(name, resolver);
                return resolved;
            }
        }
        notEngines.put(name, name);
        throw new TemplateNotFoundException(name + " not found");
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
