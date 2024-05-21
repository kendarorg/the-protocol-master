package org.kendar.proxy;

import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.ProtoState;

public abstract class NetworkProxySplitterState extends ProtoState {
    public abstract BytesEvent split(BytesEvent input);
}
