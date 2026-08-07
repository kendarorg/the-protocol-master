package org.kendar.mssql.fsm;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.DoneStatus;
import org.kendar.mssql.constants.RpcProcId;
import org.kendar.mssql.constants.TdsPacketType;
import org.kendar.mssql.dtos.RpcParam;
import org.kendar.mssql.executor.MssqlProtoContext;
import org.kendar.mssql.fsm.events.TdsPacket;
import org.kendar.mssql.messages.*;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.sql.jdbc.BindingParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Rpc extends TdsState {
    private static final Logger log = LoggerFactory.getLogger(Rpc.class);

    public Rpc(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getPacketType() {
        return TdsPacketType.RPC;
    }

    @Override
    protected Iterator<ProtoStep> executeTds(MssqlBBuffer inputBuffer, MssqlProtoContext protoContext, TdsPacket event) {
        skipAllHeaders(inputBuffer);
        var nameLength = inputBuffer.readUShortLE();
        String procName = null;
        var procId = -1;
        if (nameLength == 0xFFFF) {
            procId = inputBuffer.readUShortLE();
        } else {
            procName = new String(inputBuffer.getBytes(nameLength * 2),
                    java.nio.charset.StandardCharsets.UTF_16LE);
        }
        inputBuffer.readUShortLE(); //option flags

        var params = new ArrayList<RpcParam>();
        while (inputBuffer.getPosition() < inputBuffer.size()) {
            var paramName = inputBuffer.readBVarchar();
            var status = inputBuffer.get() & 0xFF;
            var parameter = TdsValueReader.readParam(inputBuffer);
            params.add(new RpcParam(paramName, status, parameter));
        }

        if (procName != null) {
            procId = switch (procName.toLowerCase(Locale.ROOT)) {
                case "sp_executesql" -> RpcProcId.SP_EXECUTESQL;
                case "sp_prepare" -> RpcProcId.SP_PREPARE;
                case "sp_execute" -> RpcProcId.SP_EXECUTE;
                case "sp_prepexec" -> RpcProcId.SP_PREPEXEC;
                case "sp_unprepare" -> RpcProcId.SP_UNPREPARE;
                default -> -1;
            };
        }

        var message = protoContext.newMessage();
        switch (procId) {
            case RpcProcId.SP_EXECUTESQL -> executeSql(protoContext, params, message);
            case RpcProcId.SP_PREPARE -> prepare(protoContext, params, message);
            case RpcProcId.SP_EXECUTE -> execute(protoContext, params, message);
            case RpcProcId.SP_PREPEXEC -> prepExec(protoContext, params, message);
            case RpcProcId.SP_UNPREPARE -> unprepare(protoContext, params, message);
            default -> {
                var target = procName != null ? procName : ("proc id " + procId);
                log.warn("[SERVER][RPC] Unsupported procedure {}", target);
                message.add(new ErrorToken("Unsupported RPC procedure " + target))
                        .add(new DoneProcToken(DoneStatus.DONE_ERROR, 0));
                return iteratorOfList(message);
            }
        }
        return iteratorOfList(message);
    }

    private List<BindingParameter> bindings(List<RpcParam> params, int from) {
        var result = new ArrayList<BindingParameter>();
        for (var i = from; i < params.size(); i++) {
            result.add(params.get(i).getParameter());
        }
        return result;
    }

    private String stringValue(List<RpcParam> params, int index) {
        if (index >= params.size()) return "";
        var value = params.get(index).getParameter().getValue();
        return value == null ? "" : value;
    }

    private void runStatement(MssqlProtoContext protoContext, String query,
                              List<BindingParameter> parameters, TdsReturnMessage message) {
        var ordered = new ArrayList<BindingParameter>();
        query = translateParameters(query, parameters, ordered);
        log.info("[SERVER][QUERY][2]: {}", query);
        var tokens = protoContext.getExecutor().executeQuery(protoContext, query, ordered, true);
        message.addAll(tokens);
    }

    /**
     * Replaces the driver generated placeholders (@P0..@Pn) with jdbc "?"
     * markers: the shared SqlStringParser cannot handle digits inside the
     * parameter names. Placeholders are matched back to the parameters by
     * their index, falling back to positional order
     */
    private String translateParameters(String query, List<BindingParameter> parameters,
                                       List<BindingParameter> ordered) {
        if (parameters.isEmpty()) {
            return query;
        }
        var matcher = java.util.regex.Pattern.compile("@[pP](\\d+)").matcher(query);
        var result = new StringBuilder();
        var positional = 0;
        while (matcher.find()) {
            var index = Integer.parseInt(matcher.group(1));
            if (index >= parameters.size()) {
                index = positional;
            }
            if (index < parameters.size()) {
                ordered.add(parameters.get(index));
                matcher.appendReplacement(result, "?");
                positional++;
            }
        }
        matcher.appendTail(result);
        if (ordered.isEmpty()) {
            ordered.addAll(parameters);
            return query;
        }
        return result.toString();
    }

    private void executeSql(MssqlProtoContext protoContext, List<RpcParam> params, TdsReturnMessage message) {
        var query = stringValue(params, 0);
        //params.get(1) is the parameter definitions string
        runStatement(protoContext, query, bindings(params, params.size() > 1 ? 2 : 1), message);
        message.add(new ReturnStatusToken(0))
                .add(new DoneProcToken(DoneStatus.DONE_FINAL, 0));
    }

    private void prepare(MssqlProtoContext protoContext, List<RpcParam> params, TdsReturnMessage message) {
        var query = stringValue(params, 2);
        var handle = protoContext.storePreparedStatement(query);
        message.add(new ReturnValueToken(0, params.isEmpty() ? "" : params.get(0).getName(), handle))
                .add(new ReturnStatusToken(0))
                .add(new DoneProcToken(DoneStatus.DONE_FINAL, 0));
    }

    private void execute(MssqlProtoContext protoContext, List<RpcParam> params, TdsReturnMessage message) {
        var handleValue = stringValue(params, 0);
        var handle = handleValue.isEmpty() ? -1 : Integer.parseInt(handleValue);
        var query = protoContext.getPreparedStatement(handle);
        if (query == null) {
            message.add(new ErrorToken("Invalid prepared statement handle " + handle))
                    .add(new DoneProcToken(DoneStatus.DONE_ERROR, 0));
            return;
        }
        runStatement(protoContext, query, bindings(params, 1), message);
        message.add(new ReturnStatusToken(0))
                .add(new DoneProcToken(DoneStatus.DONE_FINAL, 0));
    }

    private void prepExec(MssqlProtoContext protoContext, List<RpcParam> params, TdsReturnMessage message) {
        var query = stringValue(params, 2);
        var handle = protoContext.storePreparedStatement(query);
        message.add(new ReturnValueToken(0, params.isEmpty() ? "" : params.get(0).getName(), handle));
        runStatement(protoContext, query, bindings(params, 3), message);
        message.add(new ReturnStatusToken(0))
                .add(new DoneProcToken(DoneStatus.DONE_FINAL, 0));
    }

    private void unprepare(MssqlProtoContext protoContext, List<RpcParam> params, TdsReturnMessage message) {
        var handleValue = stringValue(params, 0);
        if (!handleValue.isEmpty()) {
            protoContext.removePreparedStatement(Integer.parseInt(handleValue));
        }
        message.add(new ReturnStatusToken(0))
                .add(new DoneProcToken(DoneStatus.DONE_FINAL, 0));
    }
}
