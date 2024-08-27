package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.enums.ConnectFlag;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class Connect extends BaseMqttState {
    private String protocolName;
    private int connectFlags;
    private short keepAlive;
    private int willQos;
    private boolean willRetain;
    private String clientId;
    private String userName;
    private String password;
    private String willTopic;
    private String willMessage;

    public Connect(MqttFixedHeader fixedHeader, String protocolName, int protocolVersion,
                   int connectFlags, short keepAlive, int willQos, boolean willRetain) {
        setFixedHeader(MqttFixedHeader.CONNECT);

        this.protocolName = protocolName;
        this.setProtocolVersion(protocolVersion);
        this.connectFlags = connectFlags;
        this.keepAlive = keepAlive;
        this.willQos = willQos;
        this.willRetain = willRetain;
    }

    public Connect() {
        super();
        setFixedHeader(MqttFixedHeader.CONNECT);
    }

    public Connect(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.CONNECT);
    }


    public String getProtocolName() {
        return protocolName;
    }

    public void setProtocolName(String protocolName) {
        this.protocolName = protocolName;
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

    public boolean isWillRetain() {
        return willRetain;
    }

    public void setWillRetain(boolean willRetain) {
        this.willRetain = willRetain;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    public String getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(String willMessage) {
        this.willMessage = willMessage;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer rb, MqttPacket event) {
        var bb = event.getBuffer();
        //Variable header
        var protocolName = bb.readUtf8String();
        var protocolVersion = (int) bb.get();

        var connectFlags = (int) bb.get();
        var keepAlive = bb.getShort();
        var context = (MqttContext) event.getContext();
        context.setProtocolVersion(protocolVersion);
        var proxy = (MqttProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var userNameFlag = ConnectFlag.isFlagSet(connectFlags, ConnectFlag.USERNAME);
        var passwordFlag = ConnectFlag.isFlagSet(connectFlags, ConnectFlag.PASSWORD);
        var willRetainFlag = ConnectFlag.isFlagSet(connectFlags, ConnectFlag.WILLRETAIN);
        var willQos = 0;
        if (ConnectFlag.isFlagSet(connectFlags, ConnectFlag.WILLQOSONE)) {
            willQos = 1;
        } else if (ConnectFlag.isFlagSet(connectFlags, ConnectFlag.WILLQOSTWO)) {
            willQos = 2;
        }
        var willFlag = ConnectFlag.isFlagSet(connectFlags, ConnectFlag.WILLFLAG);
        var cleanSession = ConnectFlag.isFlagSet(connectFlags, ConnectFlag.CLEANSESSION);
        var connect = new Connect(
                fixedHeader,
                protocolName,
                protocolVersion,
                connectFlags,
                keepAlive,
                willQos,
                willRetainFlag
        );
        //Variable header for MQTT >=5
        readProperties( connect, bb);

        connect.setFullFlag(event.getFullFlag());
        //Payload
        connect.setClientId(bb.readUtf8String());
        if (willFlag) {
            connect.setWillTopic(bb.readUtf8String());
            connect.setWillMessage(bb.readUtf8String());
        }
        if (userNameFlag) {
            connect.setUserName(bb.readUtf8String());
        }
        if (passwordFlag) {
            connect.setPassword(bb.readUtf8String());
        }
        if (cleanSession) {
            //TODOMQTT clean all sessions for connection
            //throw new RuntimeException("CLEAN SESSION");
        }

        return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                connection,
                connect,
                new ConnectAck()
        ));
    }


    @Override
    protected void writeFrameContent(MqttBBuffer rb) {

        var willFlag = ConnectFlag.isFlagSet(connectFlags, ConnectFlag.WILLFLAG);
        var userNameFlag = ConnectFlag.isFlagSet(connectFlags, ConnectFlag.USERNAME);
        var passwordFlag = ConnectFlag.isFlagSet(connectFlags, ConnectFlag.PASSWORD);
        var willRetainFlag = ConnectFlag.isFlagSet(connectFlags, ConnectFlag.WILLRETAIN);
        var cleanSession = ConnectFlag.isFlagSet(connectFlags, ConnectFlag.CLEANSESSION);
        rb.writeUtf8String(protocolName);
        rb.write((byte) getProtocolVersion());
        rb.write((byte) connectFlags);
        rb.writeShort(keepAlive);
        writeProperties(rb);
        rb.writeUtf8String(clientId);
        if (willFlag) {
            rb.writeUtf8String(willTopic);
            rb.writeUtf8String(willMessage);
        }
        if (userNameFlag) {
            rb.writeUtf8String(userName);
        }
        if (passwordFlag) {
            rb.writeUtf8String(password);
        }
    }
}
