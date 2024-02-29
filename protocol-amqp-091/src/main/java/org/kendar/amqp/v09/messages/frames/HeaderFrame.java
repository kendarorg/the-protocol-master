package org.kendar.amqp.v09.messages.frames;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.dtos.FrameType;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;
import org.kendar.amqp.v09.utils.FieldsReader;
import org.kendar.amqp.v09.utils.FieldsWriter;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import static org.kendar.amqp.v09.messages.frames.HeaderFrameField.*;

public class HeaderFrame extends Frame {

    protected static JsonMapper mapper = new JsonMapper();
    private String contentType;
    private String contentEncoding;
    private Map<String, Object> headers;
    private Integer deliveryMode;
    private Integer priority;
    private String correlationId;
    private String replyTo;
    private String expiration;
    private String messageId;
    private short weight;
    private short classId;
    private long bodySize;
    private Date timestamp;
    private String typeMsg;
    private String userId;
    private String appId;
    private String clusterId;
    private int propertyFlags;
    private int consumeId;

    public HeaderFrame() {
        setType(FrameType.HEADER.asByte());
    }

    public HeaderFrame(Class<?>... events) {
        super(events);
        setType(FrameType.HEADER.asByte());
    }

    public short getClassId() {
        return classId;
    }

    public void setClassId(short classId) {
        this.classId = classId;
    }

    public short getWeight() {
        return weight;
    }

    public void setWeight(short weight) {
        this.weight = weight;
    }

    public long getBodySize() {
        return bodySize;
    }

    public void setBodySize(long bodySize) {
        this.bodySize = bodySize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public Integer getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(Integer deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getTypeMsg() {
        return typeMsg;
    }

    public void setTypeMsg(String typeMsg) {
        this.typeMsg = typeMsg;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public int getPropertyFlags() {
        return propertyFlags;
    }

    public void setPropertyFlags(int propertyFlags) {
        this.propertyFlags = propertyFlags;
    }

    @Override
    protected void writeFrameContent(BBuffer rb) {
        rb.writeShort(classId);
        rb.writeShort(weight);
        rb.writeLong(bodySize);
        rb.writeShort((short) propertyFlags);

        boolean contentType_present = isFlagSet(propertyFlags, FLAG_CONTENT_TYPE);
        boolean contentEncoding_present = isFlagSet(propertyFlags, FLAG_CONTENT_ENCODING);
        boolean headers_present = isFlagSet(propertyFlags, FLAG_HEADERS);
        boolean deliveryMode_present = isFlagSet(propertyFlags, FLAG_DELIVERY_MODE);
        boolean priority_present = isFlagSet(propertyFlags, FLAG_PRIORITY);
        boolean correlationId_present = isFlagSet(propertyFlags, FLAG_CORRELATION_ID);
        boolean replyTo_present = isFlagSet(propertyFlags, FLAG_REPLY_TO);
        boolean expiration_present = isFlagSet(propertyFlags, FLAG_EXPIRATION);
        boolean messageId_present = isFlagSet(propertyFlags, FLAG_MESSAGE_ID);
        boolean timestamp_present = isFlagSet(propertyFlags, FLAG_TIMESTAMP);
        boolean type_present = isFlagSet(propertyFlags, FLAG_TYPE);
        boolean userId_present = isFlagSet(propertyFlags, FLAG_USER_ID);
        boolean appId_present = isFlagSet(propertyFlags, FLAG_APP_ID);
        boolean clusterId_present = isFlagSet(propertyFlags, FLAG_RESERVED1);

        if (contentType_present) new ShortStringHelper(contentType).write(rb);
        if (contentEncoding_present) new ShortStringHelper(contentEncoding).write(rb);
        if (headers_present) FieldsWriter.writeTable(headers, rb);
        if (deliveryMode_present) rb.write((byte) deliveryMode.intValue());
        if (priority_present) rb.write((byte) priority.intValue());
        if (correlationId_present) new ShortStringHelper(correlationId).write(rb);
        if (replyTo_present) new ShortStringHelper(replyTo).write(rb);
        if (expiration_present) new ShortStringHelper(expiration).write(rb);
        if (messageId_present) new ShortStringHelper(messageId).write(rb);
        if (timestamp_present) FieldsWriter.writeTimestamp(rb, timestamp);
        if (type_present) new ShortStringHelper(typeMsg).write(rb);
        if (userId_present) new ShortStringHelper(userId).write(rb);
        if (appId_present) new ShortStringHelper(appId).write(rb);
        if (clusterId_present) new ShortStringHelper(clusterId).write(rb);

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

        var hf = new HeaderFrame();
        hf.classId = rb.getShort();
        hf.weight = rb.getShort();
        hf.bodySize = rb.getLong();
        var propertyFlags = (int) rb.getShort();
        hf.propertyFlags = propertyFlags;

        boolean contentType_present = isFlagSet(propertyFlags, FLAG_CONTENT_TYPE);
        boolean contentEncoding_present = isFlagSet(propertyFlags, FLAG_CONTENT_ENCODING);
        boolean headers_present = isFlagSet(propertyFlags, FLAG_HEADERS);
        boolean deliveryMode_present = isFlagSet(propertyFlags, FLAG_DELIVERY_MODE);
        boolean priority_present = isFlagSet(propertyFlags, FLAG_PRIORITY);
        boolean correlationId_present = isFlagSet(propertyFlags, FLAG_CORRELATION_ID);
        boolean replyTo_present = isFlagSet(propertyFlags, FLAG_REPLY_TO);
        boolean expiration_present = isFlagSet(propertyFlags, FLAG_EXPIRATION);
        boolean messageId_present = isFlagSet(propertyFlags, FLAG_MESSAGE_ID);
        boolean timestamp_present = isFlagSet(propertyFlags, FLAG_TIMESTAMP);
        boolean type_present = isFlagSet(propertyFlags, FLAG_TYPE);
        boolean userId_present = isFlagSet(propertyFlags, FLAG_USER_ID);
        boolean appId_present = isFlagSet(propertyFlags, FLAG_APP_ID);
        boolean clusterId_present = isFlagSet(propertyFlags, FLAG_RESERVED1);

        hf.contentType = contentType_present ? ShortStringHelper.read(rb) : null;
        context.setValue("CONTENT_TYPE_" + channel, hf.contentType);
        hf.contentEncoding = contentEncoding_present ? ShortStringHelper.read(rb) : null;
        hf.headers = headers_present ? FieldsReader.readTable(rb) : null;
        hf.deliveryMode = deliveryMode_present ? (int) rb.get() : null;
        hf.priority = priority_present ? (int) rb.get() : null;
        hf.correlationId = correlationId_present ? ShortStringHelper.read(rb) : null;
        hf.replyTo = replyTo_present ? ShortStringHelper.read(rb) : null;
        hf.expiration = expiration_present ? ShortStringHelper.read(rb) : null;
        hf.messageId = messageId_present ? ShortStringHelper.read(rb) : null;
        hf.timestamp = timestamp_present ? FieldsReader.readTimestamp(rb) : null;
        hf.typeMsg = type_present ? ShortStringHelper.read(rb) : null;
        hf.userId = userId_present ? ShortStringHelper.read(rb) : null;
        hf.appId = appId_present ? ShortStringHelper.read(rb) : null;
        hf.clusterId = clusterId_present ? ShortStringHelper.read(rb) : null;
        hf.setChannel(channel);

        if (isProxyed()) {
            var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);
            hf.setConsumeId(basicConsume.getConsumeId());
            var storage = proxy.getStorage();
            var res = "{\"type\":\"" + hf.getClass().getSimpleName() + "\",\"data\":" +
                    mapper.serialize(hf) + "}";

            storage.write(
                    null
                    , mapper.toJsonNode(res)
                    , 0, "RESPONSE", "AMQP");
            return iteratorOfList(hf);
        }
        return iteratorOfRunnable(() -> proxy.execute(context,
                connection,
                hf
        ));
    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }
}
