package org.kendar.mqtt.plugins.apis;

import org.bouncycastle.util.encoders.Base64;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.utils.MimeChecker;
import org.kendar.exceptions.PluginException;
import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.Publish;
import org.kendar.mqtt.fsm.PublishAck;
import org.kendar.mqtt.fsm.PublishRel;
import org.kendar.mqtt.plugins.MqttPublishPlugin;
import org.kendar.mqtt.plugins.apis.dtos.MqttConnection;
import org.kendar.mqtt.plugins.apis.dtos.MqttConnections;
import org.kendar.mqtt.plugins.apis.dtos.PublishMqttMessage;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.ContentData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import static org.kendar.apis.ApiUtils.respondJson;

@HttpTypeFilter()
public class MqttPublishPluginApis extends ProtocolPluginApiHandlerDefault<MqttPublishPlugin> {
    private final MultiTemplateEngine resolversFactory;

    public MqttPublishPluginApis(MqttPublishPlugin descriptor, String id, String instanceId
            , MultiTemplateEngine resolversFactory) {
        super(descriptor, id, instanceId);
        this.resolversFactory = resolversFactory;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections")
    @TpmDoc(
            description = "Retrieve all amqp connections",
            responses = {@TpmResponse(
                    body = MqttConnection[].class,
                    description = "All active connections"
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/publish-plugin"})
    public void getConnections(Request request, Response response) {
        var result = loadConnections();
        respondJson(response, result);
    }

    private List<MqttConnection> loadConnections() {
        var pInstance = getDescriptor().getProtocolInstance();
        var result = new ArrayList<MqttConnection>();
        for (var ccache : pInstance.getContextsCache().entrySet()) {
            var key = ccache.getKey();
            var context = (MqttContext) ccache.getValue();
            var subscriptions = (HashSet<String>) context.getValue("TOPICS");
            if (subscriptions == null || subscriptions.isEmpty()) continue;

            for (var subscription : subscriptions) {
                var subSplit = subscription.split("\\|", 2);
                var qos = Integer.parseInt(subSplit[0]);
                var topic = subSplit[1];
                var connection = new MqttConnection();
                connection.setId(key);
                connection.setQos(qos);
                connection.setTopic(topic);
                connection.setLastAccess(context.getLastAccess());
                result.add(connection);
            }

        }
        return result.stream().
                sorted(Comparator.comparing(MqttConnection::getLastAccess).reversed()).toList();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections/" +
                    "{connectionId}/{topic}",
            method = "POST",
            id = "POST /api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections/" +
                    "{connectionId}/{topic}")
    @TpmDoc(
            description = "Send a message. Mandatory are only: contentType,body. If " +
                    "content type is binary, the body must be a base-64 encoded byte array.",
            path = {
                    @PathParameter(key = "connectionId", description = "Connection Id, -1 for all matching connections"),
                    @PathParameter(key = "topic", description = "Topic Id")
            },
            requests = @TpmRequest(
                    body = PublishMqttMessage.class
            ),
            responses = {@TpmResponse(
                    body = Ok.class
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/publish-plugin"})
    public void publish(Request request, Response response) {
        var messageData = mapper.deserialize(request.getRequestText().toString(), PublishMqttMessage.class);
        var connectionId = Integer.parseInt(request.getPathParameter("connectionId"));
        var topic = request.getPathParameter("topic");

        doPublish(messageData, connectionId, topic);
    }

    public void doPublish(PublishMqttMessage messageData, int connectionId, String topic) {
        var pInstance = getDescriptor().getProtocolInstance();
        byte[] dataToSend;
        if (MimeChecker.isBinary(messageData.getContentType(), null)) {
            dataToSend = Base64.decode(messageData.getBody());
        } else {
            dataToSend = messageData.getBody().getBytes();
        }

        var sentData = false;

        for (var contxtKvp : pInstance.getContextsCache().entrySet()) {
            try {
                var context = (MqttContext) contxtKvp.getValue();
                var topics = (HashSet<String>) context.getValue("TOPICS");
                if (topics == null) continue;
                var topicAvailable = topics.stream().filter(t -> t.endsWith("|" + topic)).findFirst();
                if (connectionId != -1 && connectionId != contxtKvp.getKey()) {
                    continue;
                }
                if (topicAvailable.isEmpty()) {
                    continue;
                }
                sentData = true;
                var packetIdentifier = (short) context.packetToUse();
                var message = new Publish();
                message.setPacketIdentifier(packetIdentifier);
                message.setFixedHeader(MqttFixedHeader.PUBLISH);
                message.setTopicName(topic);


                var splitTopic = topicAvailable.get().split("\\|", 2);
                var qos = Integer.parseInt(splitTopic[0]);
                message.setQos(qos);
                ContentData content = new ContentData();
                content.setBytes(dataToSend);
                message.setPayload(content);
                var realFlag = 48;
                if (qos == 1)
                    realFlag = 50;
                if (qos == 2)
                    realFlag = 52;
                //00110000
                message.setFullFlag((byte) realFlag);
                message.setFixedHeader(MqttFixedHeader.PUBLISH);
                message.setProtocolVersion(context.getProtocolVersion());
                message.asProxy();

                if (qos == 2) {
                    //should expect pubrec
                    var pubRel = new PublishRel();
                    pubRel.setPacketIdentifier(packetIdentifier);
                    pubRel.setProtocolVersion(context.getProtocolVersion());
                    pubRel.setFullFlag((byte) 98);
                    pubRel.setFixedHeader(MqttFixedHeader.PUBREL);
                    pubRel.setReasonCode((byte) 0);
                    getDescriptor().expectPubRec(context, pubRel);
                } else if (qos == 1) {
                    //should expect pubrec
                    var pubRel = new PublishAck();
                    pubRel.setPacketIdentifier(packetIdentifier);
                    pubRel.setProtocolVersion(context.getProtocolVersion());
                    pubRel.setFixedHeader(MqttFixedHeader.PUBREL);
                    pubRel.setReasonCode((byte) 0);
                    getDescriptor().expectPubAck(context, pubRel);
                }
                context.write(message);
            }catch (Exception ex){

            }
        }
        if (!sentData) {
            throw new PluginException("No existing topic to send to");
        }

    }


    @HttpMethodFilter(
            pathAddress = "/protocols/{#protocolInstanceId}/plugins/{#plugin}/connections",
            method = "GET", id = "GET /protocols/{#protocolInstanceId}/plugins/{#plugin}/connections")
    public void retrieveConnections(Request request, Response response) {

        var connections = loadConnections();
        var model = new MqttConnections();
        model.setConnections(connections);
        model.setInstanceId(getProtocolInstanceId());
        resolversFactory.render("mqtt/publish_plugin/connections.jte", model, response);
    }
}
