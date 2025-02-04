package org.kendar.mysql;

import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;

public class MySqlProtocolSettings extends JdbcProtocolSettings {
    private boolean force3BytesOkPacketInfo = false;

    public boolean isForce3BytesOkPacketInfo() {
        return force3BytesOkPacketInfo;
    }

    public void setForce3BytesOkPacketInfo(boolean force3BytesOkPacketInfo) {
        this.force3BytesOkPacketInfo = force3BytesOkPacketInfo;
    }
}
