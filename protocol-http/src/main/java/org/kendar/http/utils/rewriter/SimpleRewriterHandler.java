package org.kendar.http.utils.rewriter;

import org.kendar.http.utils.Request;

import java.net.MalformedURLException;

public interface SimpleRewriterHandler {
    Request translate(Request source) throws MalformedURLException;

    boolean ping(String host);
}
