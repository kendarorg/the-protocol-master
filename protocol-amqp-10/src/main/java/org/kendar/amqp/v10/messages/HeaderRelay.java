package org.kendar.amqp.v10.messages;

import org.kendar.amqp.v10.fsm.events.Amqp10Frame;
import org.kendar.protocol.states.InterruptProtoState;

/**
 * Relays an 8-byte AMQP/SASL protocol header the broker sends back during the
 * handshake to the client (proxy direction). Matches a buffer that starts with
 * {@code "AMQP"}; distinct from {@link EmptyFrame} (whose size field equals 8).
 */
public class HeaderRelay extends Amqp10BaseFrame implements InterruptProtoState {

    public HeaderRelay() {
        super();
    }

    public HeaderRelay(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return -1;
    }

    @Override
    public boolean canRun(Amqp10Frame event) {
        var rb = event.getBuffer();
        var pos = rb.getPosition();
        try {
            rb.setPosition(0);
            if (rb.size() < 8) {
                return false;
            }
            var b = rb.getBytes(0, 4);
            return b[0] == 'A' && b[1] == 'M' && b[2] == 'Q' && b[3] == 'P';
        } finally {
            rb.setPosition(pos);
        }
    }
}
