package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.messages.methods.Connection;
import org.kendar.amqp.v09.utils.FieldsReader;
import org.kendar.amqp.v09.utils.LongStringHelper;
import org.kendar.amqp.v09.utils.ProxySocket;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public class ConnectionStartOk extends Connection {
    private String[] auth;
    private Map<String, Object> clientProperties;
    private String[] mechanisms;
    private String[] locales;


    public ConnectionStartOk() {
        super();
    }

    public ConnectionStartOk(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 11);
    }

    public Map<String, Object> getClientProperties() {
        return clientProperties;
    }

    public void setClientProperties(Map<String, Object> clientProperties) {
        this.clientProperties = clientProperties;
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

    @Override
    protected Map<String, Object> retrieveMethodArguments() {
        return getClientProperties();
    }

    @Override
    protected void writePostArguments(BBuffer rb) {
        //FieldsWriter.writeTable(clientProperties,rb);
        new ShortStringHelper(String.join(" ", getMechanisms())).write(rb);
        rb.writeInt(auth[0].length() + auth[1].length() + 2);
        rb.write((byte) 0x00);
        rb.write(auth[0].getBytes(StandardCharsets.US_ASCII));
        rb.write((byte) 0x00);
        rb.write(auth[1].getBytes(StandardCharsets.US_ASCII));
        new ShortStringHelper(String.join(" ", getLocales())).write(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));


        var clientProperties = FieldsReader.readTable(rb);
        var mechanisms = ShortStringHelper.read(rb);
        var auth = LongStringHelper.read(rb).split("\0");
        var user = auth[1];
        var password = auth[2];
        var locale = ShortStringHelper.read(rb);

        var sock = (ProxySocket) connection.getConnection();
        var connStartOk = new ConnectionStartOk();
        connStartOk.setClientProperties(clientProperties);
        connStartOk.setMechanisms(mechanisms.split(" "));
        connStartOk.setLocales(locale.split(" "));
        connStartOk.setAuth(new String[]{proxy.getUserId(), proxy.getPassword()});

        var conTune = proxy.execute(context,
                connection,
                connStartOk,
                new ConnectionTune()
        );
        var conTuneOk = new ConnectionTuneOk();
        conTuneOk.setChannelMax(conTune.getChannelMax());
        conTuneOk.setHearthBeat(conTune.getHearthBeat());
        conTuneOk.setFrameMax(conTune.getFrameMax());


        return iteratorOfRunnable(() -> {
                    proxy.execute(context,
                            connection,
                            conTuneOk);

                    var response = new ConnectionTune();
                    response.setChannelMax((short) 0);
                    response.setFrameMax(131072);
                    response.setHearthBeat((short) 0);
                    return response;
                }
        );
    }

    public String[] getAuth() {
        return auth;
    }

    public void setAuth(String[] auth) {
        this.auth = auth;
    }
}
