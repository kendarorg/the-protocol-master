package org.kendar.mysql.messages;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CapabilityFlag;

public class Error extends MySQLReturnMessage {
    // int<1>	header	[ff] header of the ERR packet
    private int header;
    // int<2>	error_code	error-code
    private int errorCode;
    // if capabilities & CLIENT_PROTOCOL_41 {
//  string[1]	sql_state_marker	# marker of the SQL State
//  string[5]	sql_state	SQL State
// }
    private byte sqlStateMarker;

    private String sqlState;
    // string<EOF>	error_message	human readable error message
    private String errorMessage;

    private int capabilities;

    public Error() {

    }

    public int getHeader() {
        return header;
    }

    public void setHeader(int header) {
        this.header = header;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public byte getSqlStateMarker() {
        return sqlStateMarker;
    }

    public void setSqlStateMarker(byte sqlStateMarker) {
        this.sqlStateMarker = sqlStateMarker;
    }

    public String getSqlState() {
        return sqlState;
    }

    public void setSqlState(String sqlState) {
        this.sqlState = sqlState;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(int capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    protected void writeResponse(MySQLBBuffer buffer) {
        buffer.write((byte) 0xff);
        buffer.writeUB2(errorCode);
        if (CapabilityFlag.isFlagSet(capabilities, CapabilityFlag.CLIENT_PROTOCOL_41.getCode())) {
            buffer.write((byte) '#');
            buffer.write(sqlState.getBytes());
        }
        if (errorMessage != null) {
            buffer.write(errorMessage.getBytes());
        }
    }
}
