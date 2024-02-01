package org.kendar.amqp.v09.messages.frames;

import org.kendar.amqp.v09.dtos.FrameType;
import org.kendar.amqp.v09.utils.FieldsWriter;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;

import java.util.Iterator;
import java.util.Map;

public abstract class MethodFrame extends Frame{
    private short classId;

    public short getClassId() {
        return classId;
    }

    public void setClassId(short classId) {
        this.classId = classId;
    }

    public short getMethodId() {
        return methodId;
    }

    public void setMethodId(short methodId) {
        this.methodId = methodId;
    }

    private short methodId;
    public MethodFrame() {
        this.setClassAndMethod();

        setType(FrameType.METHOD.asByte());
    }

    protected abstract void setClassAndMethod();

    public MethodFrame(Class<?>...events) {
        super(events);
        this.setClassAndMethod();
        setType(FrameType.METHOD.asByte());
    }

    @Override
    protected void writeFrameContent(BBuffer rb) {
        rb.writeShort(getClassId());
        rb.writeShort(getMethodId());
        writePreArguments(rb);
        var args = retrieveMethodArguments();
        if(args!=null) {
            FieldsWriter.writeTable(args, rb);
        }
        writePostArguments(rb);
    }

    protected Map<String,Object> retrieveMethodArguments(){
        return null;
    }

    protected void writePostArguments(BBuffer rb) {

    }

    protected void writePreArguments(BBuffer rb) {

    }

    protected boolean canRunFrame(BytesEvent event) {
        var rb = event.getBuffer();
        var pos =rb.getPosition();
        var classId = rb.getShort();
        var methodId = rb.getShort();
        rb.setPosition(pos);
        return classId==getClassId() && methodId==getMethodId();
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(short channel, BBuffer rb, BytesEvent event){
        var classId = rb.getShort();
        var methodId = rb.getShort();
        return executeMethod(channel,classId,methodId,rb,event);
    }

    protected abstract Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event);

}
