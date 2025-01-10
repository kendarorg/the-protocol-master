package org.kendar.amqp.v09.messages.frames;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.context.AmqpProtoContext;
import org.kendar.amqp.v09.dtos.FrameType;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;
import org.kendar.amqp.v09.utils.AmqpProxySocket;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.PluginContext;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.ContentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class BodyFrame extends Frame {
    private static final Logger logPs = LoggerFactory.getLogger(AmqpProxySocket.class.getName());
    private ContentData content;
    private int consumeId;
    private String consumeOrigin;

    public BodyFrame() {
        setType(FrameType.BODY.asByte());
    }

    public BodyFrame(Class<?>... events) {
        super(events);
        setType(FrameType.BODY.asByte());
    }

    public ContentData getContent() {
        return content;
    }

    public void setContent(ContentData content) {
        this.content = content;
    }

    @Override
    protected void writeFrameContent(BBuffer rb) {

        rb.write(mapper.fromGenericContent(content));
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
        String ext = "[SERVER]";
        if (isProxyed()) ext = "[PROXY ]";
        logPs.debug("{}[RX]: BodyFrame Content Length: {}", ext, contentBytes.length);
        var contentItem = mapper.toGenericContent(contentBytes, contentType);


        var routingKey = context.getValue("BASIC_PUBLISH_RK_" + channel);
        var exchange = context.getValue("BASIC_PUBLISH_XC_" + channel);

        var bf = new BodyFrame();
        bf.setChannel(channel);
        bf.setContent(contentItem);
        if (isProxyed()) {
            var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);
            bf.setConsumeId(basicConsume.getConsumeId());
            var consumeOrigin = basicConsume.getQueue() + "|" + basicConsume.getChannel() + "|" + mapper.serialize(basicConsume.getArguments());
            bf.setConsumeOrigin(consumeOrigin);

            //If it is a recorder
            if (!proxy.isReplayer()) {

                proxy.respond(bf, new PluginContext("AMQP", "RESPONSE", System.currentTimeMillis(), context));
            }
            //Return itself
            return iteratorOfList(bf);
        } else if (proxy.isReplayer() && isProxyed()) {
            var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);
            bf.setConsumeId(basicConsume.getConsumeId());
        }

        return iteratorOfRunnable(() -> proxy.sendAndForget(context, connection, bf));
    }


    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }

    public String getConsumeOrigin() {
        return consumeOrigin;
    }

    public void setConsumeOrigin(String consumeOrigin) {
        this.consumeOrigin = consumeOrigin;
    }
}
