package org.kendar.amqp.v09.utils;

import org.kendar.amqp.v09.messages.frames.Frame;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

import java.util.Iterator;

import static org.kendar.protocol.states.ProtoState.iteratorOfRunnable;

public class ProxyedBehaviour {
    private static final JsonMapper mapper = new JsonMapper();

    public static Iterator<ProtoStep> doStuff(Frame input,
                                              NetworkProtoContext context, int channel,
                                              Frame toSend,
                                              NetworkProxy proxy,
                                              ProxyConnection connection) {
        if (input.isProxyed()) {
            var basicConsume = (ConsumeConnected) context.getValue("BASIC_CONSUME_CH_" + channel);
            ((ConsumeConnected) toSend).setConsumeId(basicConsume.getConsumeId());
            var storage = proxy.getStorage();
            var res = "{\"type\":\"" + toSend.getClass().getSimpleName() + "\",\"data\":" +
                    mapper.serialize(toSend) + "}";


            storage.write(
                    context.getContextId(),
                    null
                    , mapper.toJsonNode(res)
                    , 0, "RESPONSE", "AMQP");
            return Frame.iteratorOfList(toSend);
        }
        return iteratorOfRunnable(() -> {
            proxy.execute(context, connection, toSend);
        });
    }
}
