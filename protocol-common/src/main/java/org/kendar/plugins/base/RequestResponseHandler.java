package org.kendar.plugins.base;

import org.kendar.proxy.PluginContext;

public interface RequestResponseHandler<T,K> {
    boolean handle(PluginContext pluginContext, ProtocolPhase phase, T in, K out);
}
