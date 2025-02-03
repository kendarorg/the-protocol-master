package org.kendar.mysql;

import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;

public class MySqlProtocolSettings extends JdbcProtocolSettings {
    public boolean isForce3BytesOkPacketInfo() {
        return force3BytesOkPacketInfo;
    }

    public void setForce3BytesOkPacketInfo(boolean force3BytesOkPacketInfo) {
        this.force3BytesOkPacketInfo = force3BytesOkPacketInfo;
    }

    private boolean force3BytesOkPacketInfo = false;
}
