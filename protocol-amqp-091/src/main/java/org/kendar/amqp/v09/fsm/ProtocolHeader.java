package org.kendar.amqp.v09.fsm;

import org.kendar.amqp.v09.messages.methods.connection.ConnectionStart;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.ReturnMessage;
import org.kendar.protocol.fsm.ProtoState;
import org.kendar.proxy.ProxyConnection;
import org.kendar.server.SocketChannel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ProtocolHeader extends ProtoState implements ReturnMessage {
    public ProtocolHeader(Class<?>... events) {
        super(events);
    }
    public boolean canRun(BytesEvent event) {
        var prevState = event.getPrevState();
        var inputBuffer = event.getBuffer();
        var canRun = false;
        if (inputBuffer.size() < 8) {
            canRun = false;
        } else {
            var bytes = inputBuffer.getBytes(8);
            canRun = bytes[0]=='A' && bytes[1]=='M' && bytes[2]=='Q' && bytes[3]=='P'
                    && bytes[4]==0x00
                    && bytes[5]==0x00
                    && bytes[6]==0x09
                    && bytes[7]==0x01;
        }
        return canRun;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var connection = ((ProxyConnection)event.getContext().getValue("CONNECTION"));
        var sock = (SocketChannel)connection.getConnection();
        sock.write(BBuffer.of(new byte[]{'A','M','Q','P',0,0,9,1}));
        var proxyConStart = new ConnectionStart();
        sock.read(proxyConStart);
        Map<String, Object> toSendBack = new HashMap<String, Object>();
        Map<String, Object> capabilities = new HashMap<String,Object>();
        toSendBack.put("product","protocol-master");
        toSendBack.put("version","1.0.0");
        toSendBack.put("capabilities",capabilities);
        capabilities.put("consumer_priorities",true);
        capabilities.put("exchange_exchange_bindings",true);
        capabilities.put("connection.blocked",true);
        capabilities.put("authentication_failure_close",true);
        capabilities.put("per_consumer_qos",true);
        capabilities.put("basic.nack",true);
        capabilities.put("direct_reply_to",true);
        capabilities.put("publisher_confirms",true);
        capabilities.put("consumer_cancel_notify",true);

        //class id b_10
        //method id s_10
        var response = new ConnectionStart();
        response.setServerProperties(toSendBack);
        response.setMechanisms(new String[]{"AMQPLAIN","PLAIN"});
        response.setLocales(new String[]{"en_US"});
        response.setVersionMinor((byte)9);
        response.setVersionMajor((byte)0);
        return iteratorOfList(response);
    }

    @Override
    public void write(BBuffer resultBuffer) {

    }
}
