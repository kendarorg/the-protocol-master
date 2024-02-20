package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class BasicConsumeOk extends Basic {
    private String tag;

    public BasicConsumeOk() {
        super();
    }

    public BasicConsumeOk(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 21);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        new ShortStringHelper(tag).write(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
        setChannel(channel);
        this.tag = ShortStringHelper.read(rb);
        return iteratorOfList(this);
    }


}
