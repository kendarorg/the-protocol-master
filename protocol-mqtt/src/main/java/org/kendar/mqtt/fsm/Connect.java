package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.enums.ConnectFlag;
import org.kendar.mqtt.enums.Mqtt5PropertyType;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.dtos.ConnectDto;
import org.kendar.mqtt.fsm.dtos.Mqtt5Property;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.messages.ConnectAck;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.ArrayList;
import java.util.Iterator;

public class Connect extends BaseMqttState{
    public Connect() {
        super();
        setFixedHeader(MqttFixedHeader.CONNECT);
    }

    public Connect(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.CONNECT);
    }
    @Override
    protected void writeFrameContent(MqttBBuffer rb) {

    }

    @Override
    protected boolean canRunFrame(MqttPacket event) {

        return true;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer rb, MqttPacket event) {
        var bb = event.getBuffer();
        var strLength = bb.getShort();
        var protocolName = new String(bb.getBytes(strLength));
        var protocolVersion = (int)bb.get();

        var connectFlags =(int) bb.get();
        var keepAlive = bb.getShort();
        var context = (MqttContext) event.getContext();
        context.setProtocolVersion(protocolVersion);
        var proxy = (MqttProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var userNameFlag = ConnectFlag.isFlagSet(connectFlags,ConnectFlag.USERNAME);
        var passwordFlag = ConnectFlag.isFlagSet(connectFlags,ConnectFlag.PASSWORD);
        var willRetainFlag = ConnectFlag.isFlagSet(connectFlags,ConnectFlag.WILLRETAIN);
        var willQos = 0;
        if(ConnectFlag.isFlagSet(connectFlags,ConnectFlag.WILLQOSONE)){
            willQos=1;
        }else if(ConnectFlag.isFlagSet(connectFlags,ConnectFlag.WILLQOSTWO)){
            willQos=2;
        }
        var willFlag = ConnectFlag.isFlagSet(connectFlags,ConnectFlag.WILLFLAG);
        var cleanSession = ConnectFlag.isFlagSet(connectFlags,ConnectFlag.CLEANSESSION);
        var connectDto = new ConnectDto(
                protocolName,
                protocolVersion,
                connectFlags,
                keepAlive,
                willQos,
                willRetainFlag
        );
        if(context.isVersion(MqttProtocol.VERSION_5)){
            var propertiesLength = bb.readVarBInteger();
            if(propertiesLength.getValue()>0){
                connectDto.setProperties(new ArrayList<>());
                var start = bb.getPosition();
                var end = start+propertiesLength.getValue();
                while(bb.getPosition()<end){
                    var propertyType = Mqtt5PropertyType.of(bb.get());
                    connectDto.getProperties().add(new Mqtt5Property(propertyType,bb));
                }
            }
        }
        connectDto.setClientId(bb.readUtf8String());
        if(willFlag){
            connectDto.setWillTopic(bb.readUtf8String());
            connectDto.setWillMessage(bb.readUtf8String());
        }
        if(userNameFlag){
            connectDto.setUserName(bb.readUtf8String());
        }
        if(passwordFlag){
            connectDto.setPassword(bb.readUtf8String());
        }
        if(cleanSession){

            //TODOMQTT clean all sessions for connection
            throw new RuntimeException("CLEAN SESSION");
        }

        if (isProxyed()) {
            //TODOMQTT
            throw new RuntimeException("CANNOT HANDLE AS PROXY");
            //return iteratorOfEmpty();
        }
        return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                connection,
                connectDto,
                new ConnectAck()
        ));
    }
}
