package org.kendar.mqtt.fsm.dtos;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;

import java.util.List;

public class ConnectDto implements NetworkReturnMessage {
    public ConnectDto(){

    }

    public List<Mqtt5Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Mqtt5Property> properties) {
        this.properties = properties;
    }

    public void setWillRetain(boolean willRetain) {
        this.willRetain = willRetain;
    }

    private List<Mqtt5Property> properties;
    private String protocolName;
    private int protocolVersion;
    private int connectFlags;
    private short keepAlive;
    private  int willQos;
    private boolean willRetain;
    private String clientId;
    private String userName;
    private String password;
    private String willTopic;
    private String willMessage;

    public boolean isWillRetain() {
        return willRetain;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public void setProtocolName(String protocolName) {
        this.protocolName = protocolName;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public int getConnectFlags() {
        return connectFlags;
    }

    public void setConnectFlags(int connectFlags) {
        this.connectFlags = connectFlags;
    }

    public short getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(short keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getWillQos() {
        return willQos;
    }

    public void setWillQos(int willQos) {
        this.willQos = willQos;
    }

    public ConnectDto(String protocolName, int protocolVersion,
                      int connectFlags, short keepAlive, int willQos, boolean willRetain) {

        this.protocolName = protocolName;
        this.protocolVersion = protocolVersion;
        this.connectFlags = connectFlags;
        this.keepAlive = keepAlive;
        this.willQos = willQos;
        this.willRetain = willRetain;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    public String getWillTopic() {
        return willTopic;
    }

    public void setWillMessage(String willMessage) {
        this.willMessage = willMessage;
    }

    public String getWillMessage() {
        return willMessage;
    }

    @Override
    public void write(BBuffer resultBuffer) {

    }
}
