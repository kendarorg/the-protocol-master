package org.kendar.amqp.v09.apis;

import org.bouncycastle.util.encoders.Base64;
import org.kendar.amqp.v09.apis.dtos.AmqpConnection;
import org.kendar.amqp.v09.apis.dtos.PublishMessage;
import org.kendar.amqp.v09.context.AmqpProtoContext;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;
import org.kendar.amqp.v09.messages.methods.basic.BasicDeliver;
import org.kendar.amqp.v09.plugins.AmqpPublishPlugin;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.utils.MimeChecker;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.utils.ContentData;

import java.util.ArrayList;
import java.util.UUID;

import static org.kendar.apis.ApiUtils.respondJson;

@HttpTypeFilter(hostAddress = "*")
public class AmqpPublishPluginApis extends ProtocolPluginApiHandlerDefault<AmqpPublishPlugin> {
    public AmqpPublishPluginApis(AmqpPublishPlugin descriptor, String id, String instanceId) {
        super(descriptor, id, instanceId);

    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/amqp-publish-plugin/connections",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/amqp-publish-plugin/connections")
    @TpmDoc(
            description = "Retrieve all amqp connections",
            responses = {@TpmResponse(
                    body = AmqpConnection[].class,
                    description = "All active connections"
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}"})
    public void getConnections(Request request, Response response) {
        var pInstance = getDescriptor().getProtocolInstance();
        var result = new ArrayList<AmqpConnection>();
        for(var ccache:pInstance.getContextsCache().entrySet()){
            var key = ccache.getKey();
            var context = (AmqpProtoContext)ccache.getValue();
            for(var channel:context.getChannels()){

                var connection = new AmqpConnection();
                connection.setId(key);
                connection.setChannel(channel);
                var basicConsume = (BasicConsume)context.getValue("BASIC_CONSUME_CH_" + channel);
                if(basicConsume!=null){
                    connection.setExchange((String)context.getValue("EXCHANGE_CH_"+channel));
                    connection.setConsumeId(basicConsume.getConsumeId());
                    connection.setCanPublish(true);
                    connection.setConsumeOrigin(basicConsume.getConsumeOrigin());
                    connection.setConsumerTag(basicConsume.getConsumerTag());
                }
                result.add(connection);
            }
        }
        respondJson(response,result);
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/amqp-publish-plugin/connections/" +
                    "{connectionId}/{channel}",
            method = "POST",
            id = "POST /api/protocols/{#protocolInstanceId}/plugins/amqp-publish-plugin/connections/" +
                    "{connectionId}/{channel}")
    @TpmDoc(
            description = "Send a message. Mandatory are only: contentType,appId,body,binary. If " +
                    "content type is binary, the body must be a base-64 encoded byte array. ",
            path = {
                    @PathParameter(key="connectionId",description = "Connection Id"),
                    @PathParameter(key="channel",description = "Channel Id")
            },
            requests = @TpmRequest(
                    body = PublishMessage.class
            ),
            responses = {@TpmResponse(
                    body = Ok.class
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}"})
    public void publish(Request request, Response response) {
        var messageData = mapper.deserialize(request.getRequestText().toString(), PublishMessage.class);
        var connectionId = Integer.parseInt(request.getPathParameter("connectionId"));
        var channelId = Integer.parseInt(request.getPathParameter("channel"));

        doPublish(messageData, connectionId, channelId);
    }

    public void doPublish(PublishMessage messageData, int connectionId, int channelId) {
        var pInstance = getDescriptor().getProtocolInstance();
        byte[] dataToSend;
        if(MimeChecker.isBinary(messageData.getContentType(),null)){
            dataToSend = Base64.decode((String) messageData.getBody());
        }else{
            dataToSend = messageData.getBody().getBytes();
        }


        var context = pInstance.getContextsCache().get(connectionId);

        var basicConsume = (BasicConsume)context.getValue("BASIC_CONSUME_CH_" + channelId);
        var consumeId = basicConsume.getConsumeId();
        var consumeOrigin = basicConsume.getConsumeOrigin();
        var consumerTag = (String)context.getValue("BASIC_CONSUME_CT_" + basicConsume.getConsumeOrigin());
        if(consumerTag==null||consumerTag.isEmpty()){
            consumerTag = UUID.randomUUID().toString();
        }
        var exchange = (String)context.getValue("EXCHANGE_CH_"+ channelId);
        var routingKeys = (String)context.getValue("ROUTING_KEYS_CH_" + channelId);
        var bd = new BasicDeliver();
        bd.setChannel((short) channelId);
        bd.setConsumeId(basicConsume.getConsumeId());
        bd.setConsumerTag(consumerTag);
        bd.setDeliveryTag(messageData.getDeliveryTag());
        bd.setRedelivered(false);
        bd.setExchange(exchange);
        bd.setRoutingKey(routingKeys);
        bd.setConsumeOrigin(consumeOrigin);
        context.write(bd);


        var hf = new HeaderFrame();
        hf.setType((byte)2);
        hf.setClassId((short)60);
        hf.setWeight((short) 0);
        hf.setChannel((short) channelId);
        hf.setConsumeId(basicConsume.getConsumeId());
        hf.setContentType(messageData.getContentType());
        hf.setConsumeOrigin(consumeOrigin);
        hf.setAppId(messageData.getAppId());
        hf.setDeliveryMode(messageData.getDeliveryMode());
        hf.setPropertyFlags(messageData.getPropertyFlag());

        hf.setBodySize(dataToSend.length);
        context.write(hf);

        var bf = new BodyFrame();
        bf.setChannel((short) channelId);
        bf.setType((byte)3);
        bf.setConsumeId(basicConsume.getConsumeId());
        bf.setConsumeOrigin(consumeOrigin);
        ContentData content = new ContentData();
        content.setBytes(dataToSend);
        bf.setContent(content);
        context.write(bf);
    }
}
