package org.kendar.redis.plugins.apis;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.exceptions.PluginException;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.redis.Resp3Context;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.redis.parser.Resp3ParseException;
import org.kendar.redis.parser.Resp3Parser;
import org.kendar.redis.plugins.RedisPublishPlugin;
import org.kendar.redis.plugins.apis.dtos.PublishRedisMessage;
import org.kendar.redis.plugins.apis.dtos.RedisConnection;
import org.kendar.redis.plugins.apis.dtos.RedisConnections;
import org.kendar.ui.MultiTemplateEngine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.kendar.apis.ApiUtils.respondJson;


@HttpTypeFilter()
public class RedisPublishPluginApis extends ProtocolPluginApiHandlerDefault<RedisPublishPlugin> {
    private final Resp3Parser parser = new Resp3Parser();
    private final MultiTemplateEngine resolversFactory;

    public RedisPublishPluginApis(RedisPublishPlugin descriptor, String id, String instanceId, MultiTemplateEngine resolversFactory) {
        super(descriptor, id, instanceId);

        this.resolversFactory = resolversFactory;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections")
    @TpmDoc(
            description = "Retrieve all amqp connections",
            responses = {@TpmResponse(
                    body = RedisConnection[].class,
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

    private List<RedisConnection> loadConnections() {
        var pInstance = getDescriptor().getProtocolInstance();
        var result = new ArrayList<RedisConnection>();
        for (var ccache : pInstance.getContextsCache().entrySet()) {
            var key = ccache.getKey();
            var context = (Resp3Context) ccache.getValue();
            var subscription = (String) context.getValue("QUEUE");
            if (subscription == null) continue;


            var connection = new RedisConnection();
            connection.setId(key);
            connection.setSubscription(subscription);
            connection.setLastAccess(context.getLastAccess());
            result.add(connection);

        }
        return result.stream().
                sorted(Comparator.comparing(RedisConnection::getLastAccess).reversed()).toList();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections/" +
                    "{connectionId}/{queue}",
            method = "POST",
            id = "POST /api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections/" +
                    "{connectionId}/{queue}")
    @TpmDoc(
            description = "Send a message. Mandatory are only: contentType,body. If " +
                    "content type is binary, the body must be a base-64 encoded byte array.",
            path = {
                    @PathParameter(key = "connectionId", description = "Connection Id"),
                    @PathParameter(key = "queue", description = "Queue Id")
            },
            requests = @TpmRequest(
                    body = PublishRedisMessage.class
            ),
            responses = {@TpmResponse(
                    body = Ok.class
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/publish-plugin"})
    public void publish(Request request, Response response) throws Resp3ParseException {
        var messageData = mapper.deserialize(request.getRequestText().toString(), PublishRedisMessage.class);
        var connectionId = Integer.parseInt(request.getPathParameter("connectionId"));
        var queueu = request.getPathParameter("queue");

        doPublish(messageData, connectionId, queueu);
    }

    public void doPublish(PublishRedisMessage messageData, int connectionId, String queue) throws Resp3ParseException {
        var pInstance = getDescriptor().getProtocolInstance();
        String dataToSend = messageData.getBody();

        var sentData = false;
        for (var contxtKvp : pInstance.getContextsCache().entrySet()) {
            var context = (Resp3Context) contxtKvp.getValue();
            if (connectionId != -1 && connectionId != contxtKvp.getKey()) {
                continue;
            }
            sentData = true;
            var data = List.of("message", queue, dataToSend);
            var string = parser.serialize(mapper.toJsonNode(data));
            var message = new Resp3Message(context, null, data, string);
            context.write(message);
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
        var model = new RedisConnections();
        model.setConnections(connections);
        model.setInstanceId(getProtocolInstanceId());
        resolversFactory.render("redis/publish_plugin/connections.jte", model, response);
    }
}
