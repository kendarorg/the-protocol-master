package org.kendar.annotations.concrete;

import org.kendar.annotations.MatcherFunction;
import org.kendar.annotations.MatcherType;
import org.kendar.annotations.TpmMatcher;

import java.lang.annotation.Annotation;

/**
 * Concrete path matcher descriptor
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class TpmMatcherConcrete implements TpmMatcher {
    private final TpmMatcher matcher;

    public TpmMatcherConcrete(TpmMatcher matcher) {

        this.matcher = matcher;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }

    @Override
    public String value() {
        return matcher.value();
    }

    @Override
    public MatcherFunction function() {
        return matcher.function();
    }

    @Override
    public MatcherType type() {
        return matcher.type();
    }

    @Override
    public String id() {
        if (!matcher.id().isEmpty()) {
            return matcher.id();
        }
        return null;
    }
}
