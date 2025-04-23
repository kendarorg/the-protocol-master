package org.kendar.amqp.v09.plugins.apis;

import org.bouncycastle.util.encoders.Base64;
import org.kendar.amqp.v09.context.AmqpProtoContext;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;
import org.kendar.amqp.v09.messages.methods.basic.BasicDeliver;
import org.kendar.amqp.v09.plugins.AmqpPublishPlugin;
import org.kendar.amqp.v09.plugins.apis.dtos.AmqpConnection;
import org.kendar.amqp.v09.plugins.apis.dtos.AmqpConnections;
import org.kendar.amqp.v09.plugins.apis.dtos.PublishAmqpMessage;
import org.kendar.amqp.v09.plugins.apis.dtos.WhereToSend;
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
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.ContentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.kendar.apis.ApiUtils.*;

@HttpTypeFilter()
public class AmqpPublishPluginApis extends ProtocolPluginApiHandlerDefault<AmqpPublishPlugin> {
    private static final Logger log = LoggerFactory.getLogger(AmqpPublishPluginApis.class);
    private final MultiTemplateEngine resolversFactory;

    public AmqpPublishPluginApis(AmqpPublishPlugin descriptor, String id, String instanceId
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
                    body = AmqpConnection[].class,
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

    private List<AmqpConnection> loadConnections() {
        var pInstance = getDescriptor().getProtocolInstance();
        var result = new ArrayList<AmqpConnection>();
        for (var cache : pInstance.getContextsCache().entrySet()) {
            var key = cache.getKey();
            var context = (AmqpProtoContext) cache.getValue();
            for (var channel : context.getChannels()) {

                var connection = new AmqpConnection();
                connection.setId(key);
                connection.setChannel(channel);
                var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);
                if (basicConsume != null) {
                    connection.setExchange((String) context.getValue("EXCHANGE_CH_" + channel));
                    connection.setConsumeId(basicConsume.getConsumeId());
                    connection.setCanPublish(true);
                    connection.setConsumeOrigin(basicConsume.getConsumeOrigin());
                    connection.setConsumerTag(basicConsume.getConsumerTag());
                    connection.setLastAccess(context.getLastAccess());
                }
                result.add(connection);
            }
        }
        return result.stream().
                sorted(Comparator.comparing(AmqpConnection::getLastAccess).reversed()).toList();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections/" +
                    "{connectionId}/{channel}",
            method = "POST",
            id = "POST /api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections/" +
                    "{connectionId}/{channel}")
    @TpmDoc(
            description = "Send a message. Mandatory are only: contentType,appId,body,binary. If " +
                    "content type is binary, the body must be a base-64 encoded byte array. ",
            path = {
                    @PathParameter(key = "connectionId", description = "Connection Id"),
                    @PathParameter(key = "channel", description = "Channel Id")
            },
            requests = @TpmRequest(
                    body = PublishAmqpMessage.class
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
        var messageData = mapper.deserialize(request.getRequestText().toString(), PublishAmqpMessage.class);
        var connectionId = Integer.parseInt(request.getPathParameter("connectionId"));
        var channelId = Integer.parseInt(request.getPathParameter("channel"));

        var published = doPublish(messageData, connectionId, channelId);
        if (published == 0) {
            respondKo(response, "Publish failed");
        } else {
            respondOk(response);
        }
    }

    public int doPublish(PublishAmqpMessage messageData, int connectionId, int channelId) {
        var pInstance = getDescriptor().getProtocolInstance();
        byte[] dataToSend;
        if (MimeChecker.isBinary(messageData.getContentType(), null)) {
            dataToSend = Base64.decode(messageData.getBody());
        } else {
            dataToSend = messageData.getBody().getBytes();
        }
        var consumerTags = new HashSet<String>();
        var written = 0;
        for (var contextValue : pInstance.getContextsCache().entrySet()) {
            if (connectionId != 0 && !contextValue.getKey().equals(connectionId)) {
                continue;
            }
            var context = (AmqpProtoContext) contextValue.getValue();

            List<WhereToSend> basicConsumes = new ArrayList<>();
            if (channelId != 0) {
                basicConsumes = List.of(new WhereToSend((BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channelId), channelId));
            } else {
                for (var value : context.getKeys().stream().filter(val -> val.startsWith("BASIC_CONSUME_CH")).toList()) {
                    var channel = Integer.parseInt(value.replace("BASIC_CONSUME_CH_", ""));
                    basicConsumes.add(new WhereToSend((BasicConsume) context.getValue(value), channel));
                }
            }

            //From most recent
            Collections.reverse(basicConsumes);
            //{id=1, channel=1, consumeOrigin='quotations|1|{}', consumerTag=None1, canPublish=true, consumeId=1, exchange='stock'}
            for (var basicConsume : basicConsumes) {
                try {
                    var consumeId = basicConsume.getConsumeId();
                    var consumeOrigin = basicConsume.getConsumeOrigin();
                    channelId = basicConsume.getChannelId();

                    if (messageData.getQueue() != null && !messageData.getQueue().isEmpty()) {
                        if (!consumeOrigin.startsWith(messageData.getQueue() + "|")) {
                            continue;
                        }
                    }

                    var consumerTag = (String) context.getValue("BASIC_CONSUME_CT_" + basicConsume.getConsumeOrigin());
                    if (consumerTag == null || consumerTag.isEmpty()) {
                        consumerTag = UUID.randomUUID().toString();
                    } else {
                        if (consumerTags.contains(consumerTag)) {
                            continue;
                        }
                        consumerTags.add(consumerTag);
                    }
                    var exchange = (String) context.getValue("EXCHANGE_CH_" + channelId);
                    if (messageData.getExchange() != null && !messageData.getExchange().isEmpty()) {
                        if (!exchange.equalsIgnoreCase(messageData.getExchange())) {
                            continue;
                        }
                    }
                    var routingKeys = (String) context.getValue("ROUTING_KEYS_CH_" + channelId);
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
                    hf.setType((byte) 2);
                    hf.setClassId((short) 60);
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
                    bf.setType((byte) 3);
                    bf.setConsumeId(basicConsume.getConsumeId());
                    bf.setConsumeOrigin(consumeOrigin);
                    ContentData content = new ContentData();
                    content.setBytes(dataToSend);
                    bf.setContent(content);
                    context.write(bf);
                    written++;
                } catch (Exception ignored) {

                }
            }
        }
        return written;


    }

    @HttpMethodFilter(
            pathAddress = "/protocols/{#protocolInstanceId}/plugins/{#plugin}/connections",
            method = "GET", id = "GET /protocols/{#protocolInstanceId}/plugins/{#plugin}/connections")
    public void retrieveConnections(Request request, Response response) {

        var connections = loadConnections();
        var model = new AmqpConnections();
        model.setConnections(connections);
        model.setInstanceId(getProtocolInstanceId());
        resolversFactory.render("amqp091/publish_plugin/connections.jte", model, response);
    }

}
