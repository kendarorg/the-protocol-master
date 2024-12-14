package org.kendar.annotations.concrete;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.TpmMatcher;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Concrete HttpMethodFilter to ease the handling
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class HttpMethodFilterConcrete implements HttpMethodFilter {
    private final boolean methodBlocking;
    private final String pathAddress;
    private final Pattern pathPattern;
    private final String method;
    private final String description;
    private final String id;
    private final TpmMatcher[] extraMatches;

    public HttpMethodFilterConcrete(boolean methodBlocking,
                                    String pathAddress, Pattern pathPattern,
                                    String method, String description,
                                    String id, TpmMatcher[] extraMatches) {

        this.methodBlocking = methodBlocking;
        this.pathAddress = pathAddress;
        this.pathPattern = pathPattern;
        this.method = method;
        this.description = description;
        this.id = id;
        this.extraMatches = extraMatches;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }

    @Override
    public boolean blocking() {
        return methodBlocking;
    }

    @Override
    public String pathAddress() {
        return pathAddress;
    }

    @Override
    public String pathPattern() {
        if (pathPattern == null) return null;
        return pathPattern.toString();
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public TpmMatcher[] matcher() {
        var result = new ArrayList<TpmMatcher>();
        if (extraMatches != null) {
            for (var matcher :
                    extraMatches) {
                result.add(new TpmMatcherConcrete(matcher));
            }
        }
        return result.toArray(new TpmMatcher[0]);
    }
}
