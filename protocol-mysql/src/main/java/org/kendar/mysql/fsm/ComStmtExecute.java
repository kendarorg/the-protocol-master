package org.kendar.mysql.fsm;

import org.kendar.buffers.BBufferUtils;
import org.kendar.exceptions.TPMProtocolException;
import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.executor.MySQLExecutor;
import org.kendar.mysql.executor.MySQLProtoContext;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.ProxyMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.JDBCType;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Iterator;

@SuppressWarnings("MagicConstant")
public class ComStmtExecute extends ProtoState {
    private static final Logger log = LoggerFactory.getLogger(ComStmtExecute.class);

    public ComStmtExecute(Class<?>... messages) {
        super(messages);
    }

    private static void insertType(int mysqlFieldType, JDBCType fieldType, MySQLBBuffer inputBuffer, ArrayList<BindingParameter> bindingParameters, ProxyMetadata field) {
        Object value;

        switch (fieldType) {
            case BIGINT:
                value = switch (mysqlFieldType) {
                    case (1) ->//MYSQL_TYPE_TINY
                            (int) inputBuffer.get();//MYSQL_TYPE_SHORT
                    case (2), (5) ->//MYSQL_TYPE_INT24
                            inputBuffer.readUB2();//MYSQL_TYPE_LONG
                    case (3), (13) ->//MYSQL_TYPE_INT24
                            inputBuffer.readUB4();
                    case (8) ->//MYSQL_TYPE_LONGLONG
                            inputBuffer.getLong();
                    default -> throw new TPMProtocolException("MISSING MYSQL TYPE (for sql int) " + mysqlFieldType);
                };
                break;
            case BOOLEAN:
                value = inputBuffer.get() != 0;
                break;
            case CHAR:
                value = (char) inputBuffer.get();
                break;
            case DOUBLE:
                if (mysqlFieldType == 4) {
                    value = inputBuffer.getFloatLe();
                } else {
                    value = inputBuffer.getDoubleLe();
                }
                break;
            case FLOAT:
                value = inputBuffer.getFloatLe();
                break;
            case INTEGER:
                value = inputBuffer.readUB4();
                break;
            case SMALLINT:
                value = inputBuffer.readUB2();
                break;
            case TINYINT:
                value = (short) inputBuffer.get();
                break;

            case DATE:
            case TIME:
            case TIME_WITH_TIMEZONE:
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE: {
                var length = (int) inputBuffer.get();
                value = Calendar.getInstance();
                ((Calendar) value).setTimeInMillis(0);
                switch (mysqlFieldType) {
                    case (0x0a): //MYSQL_TYPE_DATE
                    case (0x0c): //MYSQL_TYPE_DATETIME
                    case (0x07): //MYSQL_TYPE_TIMESTAMP
                    {
                        switch (length) {
                            case (0):    //empty
                                ((Calendar) value).set(0, 0, 0, 0, 0, 0);
                                break;
                            case (4):    //no time
                                ((Calendar) value).set(
                                        inputBuffer.readUB2(),
                                        inputBuffer.get() - 1,
                                        inputBuffer.get(), 0, 0, 0);
                                break;
                            case (7):    //no microsec
                                ((Calendar) value).set(
                                        inputBuffer.readUB2(),
                                        inputBuffer.get() - 1,
                                        inputBuffer.get(),
                                        inputBuffer.get(),
                                        inputBuffer.get(),
                                        inputBuffer.get());
                                break;
                            default: //all fields set (11)
                                SimpleDateFormat outDateFormat = new SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss");
                                ((Calendar) value).set(
                                        inputBuffer.readUB2(),
                                        inputBuffer.get() - 1,
                                        inputBuffer.get(),
                                        inputBuffer.get(),
                                        inputBuffer.get(),
                                        inputBuffer.get());


                                String s = outDateFormat.format(((Calendar) value).getTime());
                                if (mysqlFieldType == 0x07) {
                                    var microSec = String.format("%06d", inputBuffer.readUB4());
                                    s += "." + microSec;
                                }
                                value = java.sql.Timestamp.valueOf(s);
                                break;
                        }
                        value = Timestamp.from(((Calendar) value).toInstant()).toString();
                    }
                    break;
                    case (0x0b): //MYSQL_TYPE_TIME
                    {
                        switch (length) {
                            case (0):    //empty
                                value = "00:00:00.000000";
                                break;
                            case (8):    //no microsec
                            {
                                var sign = inputBuffer.get() > 0 ? "-" : "";
                                var days = inputBuffer.readUB4() * 24;
                                value = sign + String.format("%d:%02d:%02d",
                                        inputBuffer.get() + days,
                                        inputBuffer.get(),
                                        inputBuffer.get()
                                );
                            }
                            break;
                            default: //all fields set (12)
                            {
                                var sign = inputBuffer.get() > 0 ? "-" : "";
                                var days = inputBuffer.readUB4() * 24;
                                value = sign + String.format("%d:%02d:%02d.%06d",
                                        inputBuffer.get() + days,
                                        inputBuffer.get(),
                                        inputBuffer.get(),
                                        inputBuffer.readUB4()
                                );
                            }
                            break;
                        }
                    }
                    //break;
                }
                break;
            }
            case CLOB:
            case LONGNVARCHAR:
            case LONGVARCHAR:
            case NCHAR:
            case NCLOB:
            case VARCHAR:
            case DECIMAL:
                var cdata = inputBuffer.readBytesWithLength();
                value = new String(cdata);
                break;
            default:
                value = inputBuffer.readBytesWithLength();
                break;
        }
        bindingParameters.add(new BindingParameter(value.toString(),
                field.isByteData(), field.getColumnType()));
    }

    public boolean canRun(CommandEvent event) {

        return event.getCommandType() == CommandType.COM_STMT_EXECUTE;
    }

    public Iterator<ProtoStep> execute(CommandEvent event) {
        var inputBuffer = event.getBuffer();
        var context = (MySQLProtoContext) event.getContext();
        var statementId = inputBuffer.readUB4();
        var flags = inputBuffer.get();  //TODO CURSOR
        var iterationCount = inputBuffer.readUB4();
        var query = (String) context.getValue("STATEMENT_" + statementId);
        if (query == null || query.isEmpty()) {
            return iteratorOfEmpty();
        }
        var fields = (ArrayList<ProxyMetadata>) context.getValue("STATEMENT_FIELDS_" + statementId);
        var paramFields = fields.stream().filter(f -> f.getColumnName().equalsIgnoreCase("?")).toList();
        int nullBitmapSize = (paramFields.size() + 7) / 8;
        var nullBitmap = inputBuffer.getBytes(nullBitmapSize);

        var bindingParameters = new ArrayList<BindingParameter>();
        log.debug("[SERVER][STMTEXEC]: {}", query);
        for (var i = 0; i < paramFields.size(); i++) {
            var field = paramFields.get(i);
            var isNull = BBufferUtils.getBit(nullBitmap, i) == 1;
            if (isNull) {
                bindingParameters.add(new BindingParameter(null,
                        field.isByteData(), field.getColumnType()));
            } else {
                var sendByteToServer = inputBuffer.get() == 1;
                var fieldType = 0x00;
                var parameterFlag = 0x00;
                if (sendByteToServer) {
                    fieldType = inputBuffer.get();
                    parameterFlag = inputBuffer.get();
                }

                if (field.isByteData()) {
                    var data = Base64.getEncoder().encodeToString(inputBuffer.readBytesWithLength());
                    bindingParameters.add(new BindingParameter(data,
                            field.isByteData(), field.getColumnType()));
                } else {
                    insertType(fieldType, field.getColumnType(), inputBuffer, bindingParameters, field);
                }
            }
        }

        var executor = new MySQLExecutor();
        return executor.executeText(context, query, bindingParameters, false);
    }
}
