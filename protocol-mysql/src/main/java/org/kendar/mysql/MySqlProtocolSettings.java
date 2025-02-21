package org.kendar.mysql;

import org.kendar.di.annotations.TpmService;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;

@TpmService(tags = "mysql")
public class MySqlProtocolSettings extends JdbcProtocolSettings {
    private boolean force3BytesOkPacketInfo = false;

    public boolean isForce3BytesOkPacketInfo() {
        return force3BytesOkPacketInfo;
    }

    public void setForce3BytesOkPacketInfo(boolean force3BytesOkPacketInfo) {
        this.force3BytesOkPacketInfo = force3BytesOkPacketInfo;
    }
}
