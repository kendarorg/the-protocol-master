package org.kendar.amqp.v10.fsm;

import org.kendar.amqp.v10.dtos.FrameType;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;
import org.kendar.amqp.v10.messages.RawFrame;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

/**
 * AMQP 1.0 protocol-header negotiation. Two 8-byte headers exist:
 * <ul>
 *   <li>SASL layer: {@code AMQP 0x03 1 0 0}</li>
 *   <li>AMQP layer: {@code AMQP 0x00 1 0 0}</li>
 * </ul>
 * The proxy terminates SASL (v09 credential-substitution model): on the SASL
 * header it answers the header + {@code sasl-mechanisms}, accepts the client's
 * {@code sasl-init} ({@link org.kendar.amqp.v10.messages.sasl.SaslInit}) and
 * replies {@code sasl-outcome}, then expects the second (AMQP) header. Toward the
 * broker it runs an independent SASL exchange with the proxy's configured login.
 * <p>
 * <b>M1 status:</b> this echoes the client header and forwards it upstream so the
 * FSM is wired end-to-end. The full SASL {@code mechanisms}/{@code outcome} +
 * {@code open} sequencing needs the M2 codec to build those frames and a live
 * Artemis to verify against — see protocol-amqp-10.md §5.3.
 */
public class ProtocolHeader extends ProtoState implements NetworkReturnMessage {

    public ProtocolHeader(Class<?>... events) {
        super(events);
    }

    public boolean canRun(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        inputBuffer.setPosition(0);
        if (inputBuffer.size() < 8) {
            return false;
        }
        var b = inputBuffer.getBytes(0, 8);
        inputBuffer.setPosition(0);
        return b[0] == 'A' && b[1] == 'M' && b[2] == 'Q' && b[3] == 'P'
                && (b[4] == 0x00 || b[4] == 0x03);
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var context = (NetworkProtoContext) event.getContext();
        var proxy = (NetworkProxy) context.getProxy();
        var connection = ((ProxyConnection) context.getValue("CONNECTION"));

        var inputBuffer = event.getBuffer();
        inputBuffer.setPosition(0);
        var header = inputBuffer.getBytes(0, 8);
        // Consume the 8 header bytes so the next state (and the optional 2nd
        // ProtocolHeader) don't re-match the same header (cf. Postgres SSLRequest).
        inputBuffer.truncate(8);
        var isSasl = header[4] == 0x03;
        context.setValue(isSasl ? "SASL_REQUESTED" : "SASL_DONE", true);

        // Transparent relay: forward the client's header upstream and let the broker's
        // reply header (+ SASL frames) come back through the proxy socket, relayed to
        // the client by HeaderRelay / SaslMechanisms / SaslOutcome. Wrapped in a
        // RawFrame because the proxy API takes a NetworkReturnMessage, not raw bytes.
        var forward = new RawFrame(-1, FrameType.AMQP.asByte());
        forward.setRaw(header);
        proxy.sendAndForget(context, connection, forward);
        // In broker-less replay, flush the recorded responses the replay plugin queued
        // (no-op in passthrough/record). See Amqp10BaseFrame#drainReplayResponses.
        Amqp10BaseFrame.drainReplayResponses(context);
        return iteratorOfEmpty();
    }

    @Override
    public void write(BBuffer resultBuffer) {
        // header echo is emitted via RawFrame
    }
}
