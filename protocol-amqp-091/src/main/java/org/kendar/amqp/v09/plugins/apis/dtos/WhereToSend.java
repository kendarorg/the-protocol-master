package org.kendar.amqp.v09.plugins.apis.dtos;

import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;

public class WhereToSend {
    private final BasicConsume basicConsume;
    private final int channelId;
    private final int consumeId;
    private final String consumeOrigin;

    public WhereToSend(BasicConsume basicConsume, int channelId) {
        this.basicConsume = basicConsume;
        this.consumeId = basicConsume.getConsumeId();
        this.consumeOrigin = basicConsume.getConsumeOrigin();
        this.channelId = channelId;
    }

    public BasicConsume getBasicConsume() {
        return basicConsume;
    }

    public int getChannelId() {
        return channelId;
    }

    public int getConsumeId() {
        return consumeId;
    }

    public String getConsumeOrigin() {
        return consumeOrigin;
    }
}
