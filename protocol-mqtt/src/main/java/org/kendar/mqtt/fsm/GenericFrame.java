package org.kendar.mqtt.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxySplitterState;

public class GenericFrame extends ProtoState implements NetworkReturnMessage, NetworkProxySplitterState {
    @Override
    public void write(BBuffer resultBuffer) {
        throw new RuntimeException();
    }

    public boolean canRun(BytesEvent event) {
        var rb = (MqttBBuffer) event.getBuffer();
        rb.setPosition(0);
        if (rb.size() < 1) {
            return false;
        }
        //First get the flag
        rb.get();
        var varBValue = rb.readVarBInteger();
        if (rb.size() < (1 + varBValue.getLength() + varBValue.getValue())) {
            rb.setPosition(0);
            throw new AskMoreDataException();
        }
        rb.setPosition(0);
        return true;
    }

    public BytesEvent execute(BytesEvent event) {
        var rb = (MqttBBuffer) event.getBuffer();
        rb.setPosition(0);

        //First get the flag
        var byteValue = rb.get();
        var varBValue = rb.readVarBInteger();
        if (rb.size() < (1 + varBValue.getLength() + varBValue.getValue())) {
            rb.setPosition(0);
            throw new AskMoreDataException();
        } else {
            var content = rb.getBytes((int) varBValue.getValue());
            var newBr = new MqttBBuffer(rb.getEndianness());
            newBr.write(byteValue);
            newBr.writeVarBInteger(content.length);
            newBr.write(content);
            newBr.setPosition(0);
            return new BytesEvent(null, null, newBr);
        }
    }

    @Override
    public BytesEvent split(BytesEvent input) {
        return execute(input);
    }
}
