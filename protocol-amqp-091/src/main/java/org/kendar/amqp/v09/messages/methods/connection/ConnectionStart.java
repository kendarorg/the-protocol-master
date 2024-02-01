package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.amqp.v09.utils.FieldsReader;
import org.kendar.amqp.v09.utils.LongStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;

import java.util.Iterator;
import java.util.Map;

public class ConnectionStart extends MethodFrame {
    public ConnectionStart(){super();}
    public ConnectionStart(Class<?> ...events){super(events);}


    private Map<String, Object> serverProperties;
    private String[] mechanisms;
    private String[] locales;
    private byte versionMinor;
    private byte versionMajor;

    public void setServerProperties(Map<String, Object> serverProperties) {
        this.serverProperties = serverProperties;
    }

    public Map<String, Object> getServerProperties() {
        return serverProperties;
    }

    public void setMechanisms(String[] mechanisms) {
        this.mechanisms = mechanisms;
    }

    public String[] getMechanisms() {
        return mechanisms;
    }

    public void setLocales(String[] locales) {
        this.locales = locales;
    }

    public String[] getLocales() {
        return locales;
    }

    public void setVersionMinor(byte versionMinor) {
        this.versionMinor = versionMinor;
    }

    public byte getVersionMinor() {
        return versionMinor;
    }

    public void setVersionMajor(byte versionMajor) {
        this.versionMajor = versionMajor;
    }

    public byte getVersionMajor() {
        return versionMajor;
    }


    @Override
    protected void setClassAndMethod() {
        setClassId((short) 10);
        setMethodId((short) 10);
    }

    @Override
    protected Map<String, Object> retrieveMethodArguments() {

        return getServerProperties();
    }

    @Override
    protected void writePostArguments(BBuffer rb) {
        new LongStringHelper(String.join(" ",getMechanisms())).write(rb);
        new LongStringHelper(String.join(" ",getLocales())).write(rb);
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.write(getVersionMajor());
        rb.write(getVersionMinor());
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
        this.versionMajor= rb.get();
        this.versionMinor = rb.get();
        this.serverProperties = FieldsReader.readTable(rb);
        this.mechanisms= LongStringHelper.read(rb).split(" ");
        this.locales = LongStringHelper.read(rb).split(" ");
        return iteratorOfList(this);
    }
}
