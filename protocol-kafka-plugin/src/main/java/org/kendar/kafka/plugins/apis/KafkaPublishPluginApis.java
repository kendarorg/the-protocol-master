package org.kendar.kafka.plugins.apis;

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
import org.kendar.kafka.KafkaProxy;
import org.kendar.kafka.context.KafkaContext;
import org.kendar.kafka.fsm.GenericResponse;
import org.kendar.kafka.plugins.KafkaPublishPlugin;
import org.kendar.kafka.plugins.apis.dtos.KafkaConnection;
import org.kendar.kafka.plugins.apis.dtos.KafkaConnections;
import org.kendar.kafka.plugins.apis.dtos.PublishKafkaMessage;
import org.kendar.kafka.utils.KafkaBBuffer;
import org.kendar.kafka.utils.KafkaProduceEncoder;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.proxy.ProxyConnection;
import org.kendar.ui.MultiTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondKo;
import static org.kendar.apis.ApiUtils.respondOk;

/**
 * REST + JTE surface of the Kafka publish plugin. Lists the live client
 * connections and produces a message for real through a chosen connection's
 * upstream broker socket (a v9 Produce request; protocol-kafka.md §7).
 */
@HttpTypeFilter()
public class KafkaPublishPluginApis extends ProtocolPluginApiHandlerDefault<KafkaPublishPlugin> {
    private static final Logger log = LoggerFactory.getLogger(KafkaPublishPluginApis.class);
    // High base so injected correlation ids never collide with the client's own.
    private static final AtomicInteger CORRELATION = new AtomicInteger(0x60000000);
    private final MultiTemplateEngine resolversFactory;

    public KafkaPublishPluginApis(KafkaPublishPlugin descriptor, String id, String instanceId,
                                  MultiTemplateEngine resolversFactory) {
        super(descriptor, id, instanceId);
        this.resolversFactory = resolversFactory;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections")
    @TpmDoc(
            description = "Retrieve all live Kafka client connections",
            responses = {@TpmResponse(
                    body = KafkaConnection[].class,
                    description = "All active connections"
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/publish-plugin"})
    public void getConnections(Request request, Response response) {
        respondJson(response, loadConnections());
    }

    private List<KafkaConnection> loadConnections() {
        var pInstance = getDescriptor().getProtocolInstance();
        var result = new ArrayList<KafkaConnection>();
        for (var ccache : pInstance.getContextsCache().entrySet()) {
            if (!(ccache.getValue() instanceof KafkaContext)) {
                continue;
            }
            var context = (KafkaContext) ccache.getValue();
            var connection = new KafkaConnection();
            connection.setId(ccache.getKey());
            connection.setCanPublish(context.getValue("CONNECTION") != null);
            connection.setLastAccess(context.getLastAccess());
            result.add(connection);
        }
        return result.stream()
                .sorted(Comparator.comparingLong(KafkaConnection::getLastAccess).reversed()).toList();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections/{connectionId}",
            method = "POST",
            id = "POST /api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections/{connectionId}")
    @TpmDoc(
            description = "Produce a message to a topic through a live connection. If content type is "
                    + "binary the body/key must be base-64 encoded.",
            path = {
                    @PathParameter(key = "connectionId", description = "Connection Id (0 = first available)")
            },
            requests = @TpmRequest(body = PublishKafkaMessage.class),
            responses = {@TpmResponse(
                    body = Ok.class
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/publish-plugin"})
    public void publish(Request request, Response response) {
        var messageData = mapper.deserialize(request.getRequestText().toString(), PublishKafkaMessage.class);
        var connectionId = Integer.parseInt(request.getPathParameter("connectionId"));
        if (doPublish(messageData, connectionId) == 0) {
            respondKo(response, "Publish failed (no live connection or broker error)");
        } else {
            respondOk(response);
        }
    }

    public int doPublish(PublishKafkaMessage messageData, int connectionId) {
        var pInstance = getDescriptor().getProtocolInstance();
        boolean binary = MimeChecker.isBinary(messageData.getContentType(), null);
        byte[] value = messageData.getBody() == null ? null
                : (binary ? Base64.decode(messageData.getBody())
                : messageData.getBody().getBytes(StandardCharsets.UTF_8));
        byte[] key = messageData.getKey() == null ? null
                : (binary ? Base64.decode(messageData.getKey())
                : messageData.getKey().getBytes(StandardCharsets.UTF_8));

        for (var contextValue : pInstance.getContextsCache().entrySet()) {
            if (connectionId != 0 && !contextValue.getKey().equals(connectionId)) {
                continue;
            }
            if (!(contextValue.getValue() instanceof KafkaContext)) {
                continue;
            }
            var context = (KafkaContext) contextValue.getValue();
            var proxy = (KafkaProxy) context.getProxy();
            var connection = (ProxyConnection) context.getValue("CONNECTION");
            if (proxy == null || connection == null) {
                continue;
            }
            try {
                int corr = CORRELATION.getAndIncrement();
                var frame = KafkaProduceEncoder.encode(corr, "tpm-publish",
                        messageData.getTopic(), messageData.getPartition(), key, value,
                        messageData.getAcks(), 30000, System.currentTimeMillis());
                proxy.sendBytesAndExpect(context, connection, new KafkaBBuffer(frame),
                        new GenericResponse(corr, (short) 9), true);
                return 1;
            } catch (Exception e) {
                log.error("[PUBLISH] produce failed on connection {}", contextValue.getKey(), e);
            }
        }
        return 0;
    }

    @HttpMethodFilter(
            pathAddress = "/protocols/{#protocolInstanceId}/plugins/{#plugin}/connections",
            method = "GET", id = "GET /protocols/{#protocolInstanceId}/plugins/{#plugin}/connections")
    public void retrieveConnections(Request request, Response response) {
        var model = new KafkaConnections();
        model.setConnections(loadConnections());
        model.setInstanceId(getProtocolInstanceId());
        resolversFactory.render("kafka/publish_plugin/connections.jte", model, response);
    }
}
