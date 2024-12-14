package org.kendar.apis.matchers;


import org.kendar.apis.base.Request;

public interface FilterMatcher {
    boolean matches(Request req);

    void initialize();

    boolean validate();
}
