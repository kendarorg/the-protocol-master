package org.kendar.http.rewrite;

import website.magyar.mitm.proxy.http.MitmJavaProxyHttpRequest;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpResponse;

@FunctionalInterface
public interface CustomResponderInterceptor {
    MitmJavaProxyHttpResponse intercept(MitmJavaProxyHttpRequest request);
}
