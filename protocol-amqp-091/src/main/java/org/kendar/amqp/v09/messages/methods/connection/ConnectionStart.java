package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Connection;
import org.kendar.amqp.v09.utils.FieldsReader;
import org.kendar.amqp.v09.utils.LongStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;
import java.util.Map;

public class ConnectionStart extends Connection {
    private Map<String, Object> serverProperties;
    private String[] mechanisms;
    private String[] locales;
    private byte versionMinor;
    private byte versionMajor;

    public ConnectionStart() {
        super();
    }

    public ConnectionStart(Class<?>... events) {
        super(events);
    }

    public Map<String, Object> getServerProperties() {
        return serverProperties;
    }

    public void setServerProperties(Map<String, Object> serverProperties) {
        this.serverProperties = serverProperties;
    }

    public String[] getMechanisms() {
        return mechanisms;
    }

    public void setMechanisms(String[] mechanisms) {
        this.mechanisms = mechanisms;
    }

    public String[] getLocales() {
        return locales;
    }

    public void setLocales(String[] locales) {
        this.locales = locales;
    }

    public byte getVersionMinor() {
        return versionMinor;
    }

    public void setVersionMinor(byte versionMinor) {
        this.versionMinor = versionMinor;
    }

    public byte getVersionMajor() {
        return versionMajor;
    }

    public void setVersionMajor(byte versionMajor) {
        this.versionMajor = versionMajor;
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 10);
    }

    @Override
    protected Map<String, Object> retrieveMethodArguments() {

        return getServerProperties();
    }

    @Override
    protected void writePostArguments(BBuffer rb) {
        new LongStringHelper(String.join(" ", getMechanisms())).write(rb);
        new LongStringHelper(String.join(" ", getLocales())).write(rb);
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.write(getVersionMajor());
        rb.write(getVersionMinor());
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        this.versionMajor = rb.get();
        this.versionMinor = rb.get();
        this.serverProperties = FieldsReader.readTable(rb);
        this.mechanisms = LongStringHelper.read(rb).split(" ");
        this.locales = LongStringHelper.read(rb).split(" ");
        return iteratorOfList(this);
    }
}
