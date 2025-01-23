package org.kendar.mysql.messages;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CapabilityFlag;
import org.kendar.mysql.constants.StatusFlag;

public class OkPacket extends MySQLReturnMessage {

    //  int<1>	header	[00] or [fe] the OK packet header
    private byte header;
    //  int<lenenc>	affected_rows	affected rows
    private long affectedRows;
    //  int<lenenc>	last_insert_id	last insert-id
    private long lastInsertId;
    //  if capabilities & CLIENT_PROTOCOL_41 {
//     int<2>	status_flags	Status Flags
//     int<2>	warnings	number of warnings
//    } elseif capabilities & CLIENT_TRANSACTIONS {
//     int<2>	status_flags	Status Flags
//    }
    private int statusFlags;
    private int warnings;
    //  if capabilities & CLIENT_SESSION_TRACK {
//           string<lenenc>	info	human readable status information
//        if status_flags & SERVER_SESSION_STATE_CHANGED {
//           string<lenenc>	session_state_changes	session state info
//        }
//    } else {
//           string<EOF>	info	human readable status information
//    }
    private String info;
    private String sessionStateChanges;

    private int capabilities;
    private int extendedCapabilities;

    public int getExtendedCapabilities() {
        return extendedCapabilities;
    }

    public void setExtendedCapabilities(int extendedCapabilities) {
        this.extendedCapabilities = extendedCapabilities;
    }

    public byte getHeader() {
        return header;
    }

    public void setHeader(byte header) {
        this.header = header;
    }

    public long getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(long affectedRows) {
        this.affectedRows = affectedRows;
    }

    public long getLastInsertId() {
        return lastInsertId;
    }

    public void setLastInsertId(long lastInsertId) {
        this.lastInsertId = lastInsertId;
    }

    public int getStatusFlags() {
        return statusFlags;
    }

    public void setStatusFlags(int statusFlags) {
        this.statusFlags = statusFlags;
    }

    public OkPacket withStatusFlag(int statusFlags) {
        setStatusFlags(statusFlags);
        return this;
    }

    public OkPacket withLastInsertedId(int lastInsertedId) {
        setLastInsertId(lastInsertedId);
        return this;
    }

    public int getWarnings() {
        return warnings;
    }

    public void setWarnings(int warnings) {
        this.warnings = warnings;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getSessionStateChanges() {
        return sessionStateChanges;
    }

    public void setSessionStateChanges(String sessionStateChanges) {
        this.sessionStateChanges = sessionStateChanges;
    }

    public int getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(int capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    protected void writeResponse(MySQLBBuffer resultBuffer) {
        resultBuffer.write((byte) 0x00);
        resultBuffer.writeLength(this.affectedRows);
        resultBuffer.writeLength(this.lastInsertId);
        if (CapabilityFlag.isFlagSet(capabilities, CapabilityFlag.CLIENT_PROTOCOL_41)) {
            resultBuffer.writeUB2(this.statusFlags);
            resultBuffer.writeUB2(this.warnings);
        } else if (CapabilityFlag.isFlagSet(capabilities, CapabilityFlag.CLIENT_TRANSACTIONS)) {
            resultBuffer.writeUB2(this.statusFlags);
        }
        if (CapabilityFlag.isFlagSet(extendedCapabilities, CapabilityFlag.CLIENT_SESSION_TRACK)) {
            resultBuffer.writeWithLength(this.info.getBytes());
            if (StatusFlag.isFlagSet(statusFlags, StatusFlag.SERVER_SESSION_STATE_CHANGED)) {
                resultBuffer.writeWithLength(this.sessionStateChanges.getBytes());
            }
        } else {

            if (info != null) {
                resultBuffer.writeWithLength(info.getBytes());
                //resultBuffer.write((byte)0);
            } else {
                resultBuffer.writeWithLength(new byte[]{0, 0, 0});
            }
        }
    }
}
