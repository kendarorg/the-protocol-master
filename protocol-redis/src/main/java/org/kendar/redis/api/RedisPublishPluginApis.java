package org.kendar.redis.api;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.redis.Resp3Context;
import org.kendar.redis.api.dto.PublishRedisMessage;
import org.kendar.redis.api.dto.RedisConnection;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.redis.plugins.RedisPublishPlugin;

import java.util.ArrayList;
import java.util.List;

import static org.kendar.apis.ApiUtils.respondJson;


@HttpTypeFilter()
public class RedisPublishPluginApis extends ProtocolPluginApiHandlerDefault<RedisPublishPlugin> {
    public RedisPublishPluginApis(RedisPublishPlugin descriptor, String id, String instanceId) {
        super(descriptor, id, instanceId);

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
            tags = {"plugins/{#protocol}/{#protocolInstanceId}"})
    public void getConnections(Request request, Response response) {
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
            result.add(connection);

        }
        respondJson(response, result);
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
            tags = {"plugins/{#protocol}/{#protocolInstanceId}"})
    public void publish(Request request, Response response) {
        var messageData = mapper.deserialize(request.getRequestText().toString(), PublishRedisMessage.class);
        var connectionId = Integer.parseInt(request.getPathParameter("connectionId"));
        var queueu = request.getPathParameter("queue");

        doPublish(messageData, connectionId, queueu);
    }

    public void doPublish(PublishRedisMessage messageData, int connectionId, String queue) {
        var pInstance = getDescriptor().getProtocolInstance();
        String dataToSend = messageData.getBody();

        for (var contxtKvp : pInstance.getContextsCache().entrySet()) {
            var context = (Resp3Context) contxtKvp.getValue();
            if (connectionId != -1 && connectionId != contxtKvp.getKey()) {
                continue;
            }
            var message = new Resp3Message(context, null,
                    mapper.toJsonNode(List.of("message", queue, dataToSend)));
            context.write(message);
        }


    }
}
