package org.kendar.postgres.fsm;

import io.netty.buffer.Unpooled;
import org.kendar.buffers.BBufferUtils;
import org.kendar.iterators.ProcessId;
import org.kendar.postgres.messages.NoticeReponse;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.SSLHandshake;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;
import org.kendar.tcpserver.NettyServerChannel;
import org.kendar.utils.Sleeper;

import java.util.EventListener;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class SSLRequest extends ProtoState {
    public static final byte[] SSL_MESSAGE_MARKER = BBufferUtils.toByteArray(0x04, 0xd2, 0x16, 0x2f);

    public SSLRequest(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        return inputBuffer.contains(SSL_MESSAGE_MARKER, 4);
    }


    public Iterator<ProtoStep> execute(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        var protoContext = event.getContext();
        var postgresContext = (PostgresProtoContext) protoContext;
        var pid = (ProcessId) protoContext.getValue("PG_PID");
        if (pid == null) {
            pid = new ProcessId(postgresContext.getPid());
            protoContext.setValue("PG_PID", pid);
        }

        inputBuffer.truncate(8);
        var jdbcSettings = (JdbcProtocolSettings) event.getContext().getDescriptor().getSettings();
        if(jdbcSettings.isUseTls()) {
            event.getContext().setValue("SSL", true);
            //event.getContext().setValue("BYPASS_CONVERTERS", true);
            //event.getContext().setValue("INITALIZE_SSL_HANDSHAKE", true);
            var context = (NetworkProtoContext) event.getContext();
            var client = (NettyServerChannel) context.getClient();
            var sslContext = context.getSslContext();
            var ctx = client.getChannelHandlerContext();
            //NoticeReponse('S')
            ctx.writeAndFlush(Unpooled.wrappedBuffer(new byte[]{'S'}));
            SSLHandshake.initializeSslUpdate(event);
            return iteratorOfList();
        }
        return iteratorOfList(new NoticeReponse());
    }
}
