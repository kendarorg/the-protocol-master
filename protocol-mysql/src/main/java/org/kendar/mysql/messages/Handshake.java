package org.kendar.mysql.messages;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.Language;

import java.nio.charset.StandardCharsets;

/**
 * Even called HandshakePacket
 */
public class Handshake extends MySQLReturnMessage {
    public static final byte[] RESERVED_FILL = new byte[10];
    private byte protocolNumber;
    private String serverVersion;
    private int threadId;
    private short serverCapabilities;
    private Language serverLanguage;
    private short serverStatus;
    private short extendedServerCapabilities;
    private String authenticationPlugin;

    public byte getProtocolNumber() {
        return protocolNumber;
    }

    public void setProtocolNumber(byte protocolNumber) {
        this.protocolNumber = protocolNumber;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public short getServerCapabilities() {
        return serverCapabilities;
    }

    public void setServerCapabilities(short serverCapabilities) {
        this.serverCapabilities = serverCapabilities;
    }

    public Language getServerLanguage() {
        return serverLanguage;
    }

    public void setServerLanguage(Language serverLanguage) {
        this.serverLanguage = serverLanguage;
    }

    public short getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(short serverStatus) {
        this.serverStatus = serverStatus;
    }

    public short getExtendedServerCapabilities() {
        return extendedServerCapabilities;
    }

    public void setExtendedServerCapabilities(short extendedServerCapabilities) {
        this.extendedServerCapabilities = extendedServerCapabilities;
    }

    public String getAuthenticationPlugin() {
        return authenticationPlugin;
    }

    public void setAuthenticationPlugin(String authenticationPlugin) {
        this.authenticationPlugin = authenticationPlugin;
    }

    @Override
    public void writeResponse(MySQLBBuffer resultBuffer) {
        resultBuffer.write(protocolNumber);
        resultBuffer.write( //"5.6.40-log".getBytes(StandardCharsets.US_ASCII));
                serverVersion.getBytes(StandardCharsets.US_ASCII));
        resultBuffer.write((byte) 0x00);
        resultBuffer.writeUB4(//55978868);
                threadId);
        resultBuffer.write(new byte[]{'B', 'W', 'p', 'M', 'm', 'a', '}', 's', 0x00});
        resultBuffer.writeUB2(serverCapabilities);
        resultBuffer.write((byte) serverLanguage.getValue());
        resultBuffer.writeUB2(serverStatus);
        resultBuffer.writeUB2(extendedServerCapabilities);
        resultBuffer.write((byte) authenticationPlugin.length());
        resultBuffer.write(RESERVED_FILL);
        resultBuffer.write(new byte[]{'H', 'N', '.', '9', 't', ']', 'V', 'w', 's', '[', 'N', 'V', 0x00});
        resultBuffer.write(authenticationPlugin.getBytes(StandardCharsets.US_ASCII));
        resultBuffer.write((byte) 0x00);
    }
}
