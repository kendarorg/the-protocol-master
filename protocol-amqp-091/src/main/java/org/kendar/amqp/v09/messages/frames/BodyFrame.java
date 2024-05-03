package org.kendar.amqp.v09.messages.frames;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.dtos.FrameType;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;
import org.kendar.amqp.v09.utils.ProxySocket;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class BodyFrame extends Frame {
    private static final Logger log = LoggerFactory.getLogger(BodyFrame.class);
    private static final Logger logPs = LoggerFactory.getLogger(ProxySocket.class.getName());
    protected static final JsonMapper mapper = new JsonMapper();
    private byte[] contentBytes;
    private String contentString;
    private int consumeId;

    public BodyFrame() {
        setType(FrameType.BODY.asByte());
    }


    public BodyFrame(Class<?>... events) {
        super(events);
        setType(FrameType.BODY.asByte());
    }

    @Override
    protected void writeFrameContent(BBuffer rb) {

        rb.write(contentBytes);
    }

    @Override
    protected boolean canRunFrame(AmqpFrame event) {
        return true;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(short channel, BBuffer rb, AmqpFrame event, int size) {

        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));
        var contentType = (String) event.getContext().getValue("CONTENT_TYPE_" + channel);
        var contentBytes = rb.getBytes(size);
        String contentString = null;
        String ext = "[SERVER]";
        if (isProxyed()) ext = "[PROXY ]";
        logPs.debug(ext + "[RX]: BodyFrame Content Length: " + contentBytes.length);
        if (contentType != null) {
            logPs.debug(ext + "[RX]: BodyFrame Content type: " + contentType);
            if (contentType.contains("text") ||
                    contentType.contains("json") ||
                    contentType.contains("xml")) {
                contentString = new String(contentBytes);
                logPs.debug(ext + "[RX]: BodyFrame Content: " + contentString);
            }
        }


        var bf = new BodyFrame();
        bf.setChannel(channel);
        bf.setContentBytes(contentBytes);
        bf.setContentString(contentString);
        if (isProxyed()) {
            var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);
            bf.setConsumeId(basicConsume.getConsumeId());


            //If it is not a replayer do not save
            if (!proxy.isReplayer()) {
                var storage = proxy.getStorage();
                var res = "{\"type\":\"" + bf.getClass().getSimpleName() + "\",\"data\":" +
                        mapper.serialize(bf) + "}";
                //Write it down
                storage.write(
                        context.getContextId(),
                        null
                        , mapper.toJsonNode(res)
                        , 0, "RESPONSE", "AMQP");
            }
            //Return itself
            return iteratorOfList(bf);
        } else if (proxy.isReplayer() && isProxyed()) {
            var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);
            bf.setConsumeId(basicConsume.getConsumeId());
        }

        return iteratorOfRunnable(() -> proxy.execute(context, connection, bf));
    }

    public byte[] getContentBytes() {
        return contentBytes;
    }

    public void setContentBytes(byte[] contentBytes) {
        this.contentBytes = contentBytes;
    }

    public String getContentString() {
        return contentString;
    }

    public void setContentString(String contentString) {
        this.contentString = contentString;
    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }
}
