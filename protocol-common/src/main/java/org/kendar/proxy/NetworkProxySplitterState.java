package org.kendar.proxy;

import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.events.ProtocolEvent;

public interface NetworkProxySplitterState {
    BytesEvent split(BytesEvent input);

    boolean canRunEvent(ProtocolEvent event);
}
