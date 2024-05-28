package org.kendar.redis.fsm;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.ProxyConnection;
import org.kendar.redis.Resp3Context;
import org.kendar.redis.Resp3Proxy;
import org.kendar.redis.fsm.events.ProxyResp3Message;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Resp3PullState extends ProtoState implements NetworkReturnMessage {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(Resp3PullState.class);
    private Resp3Message event;
    private boolean proxy;

    public Resp3PullState() {
        super();
    }

    public Resp3PullState(Class<?>... events) {
        super(events);
    }

    public Resp3Message getEvent() {
        return event;
    }

    public Resp3PullState asProxy() {
        this.proxy = true;
        return this;
    }

    public boolean isProxyed() {
        return proxy;
    }

    @Override
    public void write(BBuffer resultBuffer) {
        try {
            var bytes = event.getMessage().getBytes(StandardCharsets.US_ASCII);
            resultBuffer.write(bytes);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    public boolean canRun(Resp3Message event) {
        if (isProxyed() && event.getData() instanceof List) {
            if (((List<?>) event.getData()).get(0) != null && ((List<?>) event.getData()).get(0).toString().equalsIgnoreCase("message")) {
                return true;
            }

        }
        if (isProxyed()) {
            return false;
        }
        return true;
    }



    public Iterator<ProtoStep> execute(Resp3Message event) {
        var context = (Resp3Context) event.getContext();
        var proxy = (Resp3Proxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        return iteratorOfRunnable(() -> proxy.execute(context,
                connection,
                event
        ));
    }

    public boolean canRun(ProxyResp3Message event) {

//        if(event.getData() instanceof List<?>) {
//            if (((List<?>) event.getData()).get(0) != null && ((List<?>) event.getData()).get(0).toString().
//                    equalsIgnoreCase("message")) {
//                return true;
//            }
//        }

        return true;
    }

    public Iterator<ProtoStep> execute(ProxyResp3Message event) {
        var context = (Resp3Context) event.getContext();
        var proxy = (Resp3Proxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));
        var storage = proxy.getStorage();
        var request = (Map<String,Object>)context.getValue("REQUEST");

        if (event.getData() instanceof List) {
            if (((List<?>) event.getData()).get(0) != null && ((List<?>) event.getData()).get(0).toString().
                    equalsIgnoreCase("message")) {

                var res = "{\"type\":\"RESPONSE\",\"data\":" +
                        mapper.serialize(event.getData()) + "}";

                request.clear();

                storage.write(
                        context.getContextId(),
                        null
                        , mapper.toJsonNode(res)
                        , 0, "RESPONSE", "RESP3");

            }

        }
        if(!request.isEmpty()){
            var res = "{\"type\":\"" + event.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(event.getData()) + "}";
            long end = System.currentTimeMillis();
            /*context.setValue("REQUEST", (Map<String,Object>)Map.of(
                "index",index,
                "contextId",context.getContextId(),
                "req",mapper.toJsonNode(req),
                "start",start,
                "class",of.getClass().getSimpleName(),
                "caller",getCaller()
        ));*/
            storage.write(
                    (long)request.get("index"),
                    context.getContextId(),
                    (JsonNode) request.get("req")
                    , mapper.toJsonNode(res)
                    , end-(long)request.get("start"),
                    (String)request.get("class"),
                    (String)request.get("caller"));
        }
        return iteratorOfList(event);
//        if (!this.proxy) {
//            return iteratorOfRunnable(() -> proxy.execute(context,
//                    connection,
//                    event,
//                    new Resp3PullState().asProxy()
//            ));
//        } else {
//            this.event = event;
//            return iteratorOfList(event);
//        }
    }
}
