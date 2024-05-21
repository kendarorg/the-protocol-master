package org.kendar.proxy;

import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.events.BytesEvent;

public interface NetworkProxySplitterState  {
    BytesEvent split(BytesEvent input);
    boolean canRunEvent(BaseEvent event);
}
