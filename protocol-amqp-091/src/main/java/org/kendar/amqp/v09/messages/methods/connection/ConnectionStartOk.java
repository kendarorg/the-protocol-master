package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.amqp.v09.utils.FieldsReader;
import org.kendar.amqp.v09.utils.LongStringHelper;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.server.SocketChannel;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public class ConnectionStartOk extends MethodFrame {
    private String[] auth;

    public ConnectionStartOk(){super();}
    public ConnectionStartOk(Class<?> ...events){super(events);}

    @Override
    protected void setClassAndMethod() {
        setClassId((short) 10);
        setMethodId((short) 11);
    }



    private Map<String, Object> clientProperties;
    private String[] mechanisms;
    private String[] locales;

    public void setClientProperties(Map<String, Object> clientProperties) {
        this.clientProperties = clientProperties;
    }

    public Map<String, Object> getClientProperties() {
        return clientProperties;
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


    @Override
    protected Map<String, Object> retrieveMethodArguments() {
        return getClientProperties();
    }

    @Override
    protected void writePostArguments(BBuffer rb) {
         //FieldsWriter.writeTable(clientProperties,rb);
        new ShortStringHelper(String.join(" ",getMechanisms())).write(rb);
        rb.writeInt(auth[0].length()+auth[1].length()+2);
        rb.write((byte)0x00);
        rb.write(auth[0].getBytes(StandardCharsets.US_ASCII));
        rb.write((byte)0x00);
        rb.write(auth[1].getBytes(StandardCharsets.US_ASCII));
        new ShortStringHelper(String.join(" ",getLocales())).write(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
        var context = (AmqpProtoContext)event.getContext();
        var connection = ((ProxyConnection)event.getContext().getValue("CONNECTION"));
        var proxy = (AmqpProxy)event.getContext().getProxy();



        var clientProperties = FieldsReader.readTable(rb);
        var mechanisms = ShortStringHelper.read(rb);
        var auth = LongStringHelper.read(rb).split("\0");
        var user = auth[1];
        var password = auth[2];
        var locale = ShortStringHelper.read(rb);

        var sock = (SocketChannel)connection.getConnection();
        var connStartOk = new ConnectionStartOk();
        connStartOk.setClientProperties(clientProperties);
        connStartOk.setMechanisms(mechanisms.split(" "));
        connStartOk.setLocales(locale.split(" "));
        connStartOk.setAuth(new String[]{proxy.getUserId(),proxy.getPassword()});
        sock.write(connStartOk,context.buildBuffer());
        var conTune = new ConnectionTune();
        sock.read(conTune);
        var conTuneOk = new ConnectionTuneOk();
        conTuneOk.setChannelMax(conTune.getChannelMax());
        conTuneOk.setHearthBeat(conTune.getHearthBeat());
        conTuneOk.setFrameMax(conTune.getFrameMax());
        sock.write(conTuneOk,context.buildBuffer());
//        throw new RuntimeException("MISSING AUT");
        //connStartOk.set

        var response = new ConnectionTune();
        response.setChannelMax((short) 0);
        response.setFrameMax(131072);
        response.setHearthBeat((short) 0);
        return iteratorOfList(response);
    }


    public void setAuth(String[] auth) {
        this.auth = auth;
    }

    public String[] getAuth() {
        return auth;
    }
}
