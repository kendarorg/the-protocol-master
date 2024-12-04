package org.kendar.annotations.concrete;

import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.*;

import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
public class TpmDocConcrete implements TpmDoc {
    private final TpmDoc doc;
    private final String[] tags;

    public TpmDocConcrete(TpmDoc doc) {

        this.doc = doc;
        this.tags = doc.tags();
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }

    @Override
    public String[] tags() {
        return tags;
    }

    @Override
    public boolean todo() {
        return false;
    }

    @Override
    public String description() {
        return doc.description();
    }

    @Override
    public String produce() {
        return doc.produce();
    }

    @Override
    public QueryString[] query() {
        return doc.query();
    }

    @Override
    public PathParameter[] path() {
        return doc.path();
    }

    @Override
    public Header[] header() {
        return doc.header();
    }

    @Override
    public TpmRequest[] requests() {
        return doc.requests();
    }

    @Override
    public TpmResponse[] responses() {
        return doc.responses();
    }

    @Override
    public TpmSecurity[] security() {
        return doc.security();
    }
}
