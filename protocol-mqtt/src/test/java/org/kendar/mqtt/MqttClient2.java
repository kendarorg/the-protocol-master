package org.kendar.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingReq;

import java.lang.reflect.Field;

public class MqttClient2 extends MqttClient {

    public MqttClient2(String serverURI, String clientId,   MqttClientPersistence persistence) throws MqttException {
        super(serverURI, clientId, persistence);
    }

    public MqttClient2(String serverURI, String publisherId) throws MqttException {
        super(serverURI, publisherId);
    }

    public void pingreq()  throws MqttException {

        try {
            MqttDeliveryToken token = new MqttDeliveryToken(getClientId());
            MqttPingReq pingMsg = new MqttPingReq();
            Field privateField
                    = aClient.getClass().getDeclaredField("comms");
            // Set the accessibility as true
            privateField.setAccessible(true);

            // Store the value of private field in variable
            var name = (ClientComms)privateField.get(aClient);

            name.sendNoWait(pingMsg, token);
        }catch (NoSuchFieldException |IllegalAccessException ex){
            throw new MqttException(ex);
        }

    }
}