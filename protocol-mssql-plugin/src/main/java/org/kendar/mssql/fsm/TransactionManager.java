package org.kendar.mssql.fsm;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.DoneStatus;
import org.kendar.mssql.constants.EnvChangeType;
import org.kendar.mssql.constants.TdsPacketType;
import org.kendar.mssql.constants.TmRequestType;
import org.kendar.mssql.executor.MssqlProtoContext;
import org.kendar.mssql.fsm.events.TdsPacket;
import org.kendar.mssql.messages.DoneToken;
import org.kendar.mssql.messages.EnvChangeTransactionToken;
import org.kendar.mssql.messages.ErrorToken;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.sql.jdbc.JdbcProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class TransactionManager extends TdsState {
    private static final long TRANSACTION_DESCRIPTOR = 1;
    private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);

    public TransactionManager(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getPacketType() {
        return TdsPacketType.TRANSACTION_MANAGER;
    }

    @Override
    protected Iterator<ProtoStep> executeTds(MssqlBBuffer inputBuffer, MssqlProtoContext protoContext, TdsPacket event) {
        skipAllHeaders(inputBuffer);
        var requestType = inputBuffer.readUShortLE();
        var proxy = (JdbcProxy) protoContext.getProxy();
        var message = protoContext.newMessage();
        log.debug("[SERVER][TM] request {}", requestType);
        switch (requestType) {
            case TmRequestType.TM_BEGIN_XACT -> {
                proxy.executeBegin(protoContext);
                protoContext.setValue("TRANSACTION", true);
                message.add(new EnvChangeTransactionToken(EnvChangeType.BEGIN_TRANSACTION,
                        TRANSACTION_DESCRIPTOR, 0));
            }
            case TmRequestType.TM_COMMIT_XACT -> {
                proxy.executeCommit(protoContext);
                protoContext.setValue("TRANSACTION", false);
                message.add(new EnvChangeTransactionToken(EnvChangeType.COMMIT_TRANSACTION,
                        0, TRANSACTION_DESCRIPTOR));
            }
            case TmRequestType.TM_ROLLBACK_XACT -> {
                proxy.executeRollback(protoContext);
                proxy.executeCommit(protoContext);
                protoContext.setValue("TRANSACTION", false);
                message.add(new EnvChangeTransactionToken(EnvChangeType.ROLLBACK_TRANSACTION,
                        0, TRANSACTION_DESCRIPTOR));
            }
            default -> {
                message.add(new ErrorToken("Unsupported transaction manager request " + requestType))
                        .add(new DoneToken(DoneStatus.DONE_ERROR, 0));
                return iteratorOfList(message);
            }
        }
        message.add(new DoneToken(DoneStatus.DONE_FINAL, 0));
        return iteratorOfList(message);
    }
}
