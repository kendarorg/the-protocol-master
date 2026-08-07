package org.kendar.amqp.v10.plugins.apis;

import org.bouncycastle.util.encoders.Base64;
import org.kendar.amqp.v10.codec.Amqp10Binary;
import org.kendar.amqp.v10.codec.Amqp10Frames;
import org.kendar.amqp.v10.codec.Amqp10TypeWriter;
import org.kendar.amqp.v10.codec.DescribedType;
import org.kendar.amqp.v10.codec.UnsignedInt;
import org.kendar.amqp.v10.codec.UnsignedLong;
import org.kendar.amqp.v10.context.Amqp10ProtoContext;
import org.kendar.amqp.v10.dtos.FrameType;
import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.RawFrame;
import org.kendar.amqp.v10.plugins.Amqp10PublishPlugin;
import org.kendar.amqp.v10.plugins.apis.dtos.Amqp10Connection;
import org.kendar.amqp.v10.plugins.apis.dtos.Amqp10Connections;
import org.kendar.amqp.v10.plugins.apis.dtos.PublishAmqp10Message;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.utils.MimeChecker;
import org.kendar.buffers.BBuffer;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.ui.MultiTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondKo;
import static org.kendar.apis.ApiUtils.respondOk;

/**
 * REST + JTE surface of the AMQP 1.0 publish plugin. Lists the consumer delivery
 * links known to the proxy and injects a {@code transfer} (plus a single message
 * section) onto a chosen link — the 1.0 analog of v09's {@code AmqpPublishPluginApis}.
 */
@HttpTypeFilter()
public class Amqp10PublishPluginApis extends ProtocolPluginApiHandlerDefault<Amqp10PublishPlugin> {
    private static final Logger log = LoggerFactory.getLogger(Amqp10PublishPluginApis.class);
    private final MultiTemplateEngine resolversFactory;
    private final Amqp10TypeWriter writer = new Amqp10TypeWriter();

    public Amqp10PublishPluginApis(Amqp10PublishPlugin descriptor, String id, String instanceId,
                                   MultiTemplateEngine resolversFactory) {
        super(descriptor, id, instanceId);
        this.resolversFactory = resolversFactory;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections")
    @TpmDoc(
            description = "Retrieve all amqp 1.0 consumer links",
            responses = {@TpmResponse(
                    body = Amqp10Connection[].class,
                    description = "All active consumer links"
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/publish-plugin"})
    public void getConnections(Request request, Response response) {
        respondJson(response, loadConnections());
    }

    private List<Amqp10Connection> loadConnections() {
        var pInstance = getDescriptor().getProtocolInstance();
        var result = new ArrayList<Amqp10Connection>();
        for (var ccache : pInstance.getContextsCache().entrySet()) {
            if (!(ccache.getValue() instanceof Amqp10ProtoContext)) {
                continue;
            }
            var context = (Amqp10ProtoContext) ccache.getValue();
            for (var link : context.getReceiverLinks().values()) {
                var connection = new Amqp10Connection();
                connection.setId(ccache.getKey());
                connection.setChannel(link.getChannel());
                connection.setHandle(link.getHandle());
                connection.setLinkName(link.getName());
                connection.setSource(link.getSource());
                connection.setCanPublish(true);
                connection.setLastAccess(link.getLastAccess());
                result.add(connection);
            }
        }
        return result.stream()
                .sorted(Comparator.comparingLong(Amqp10Connection::getLastAccess).reversed()).toList();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections/" +
                    "{connectionId}/{channel}",
            method = "POST",
            id = "POST /api/protocols/{#protocolInstanceId}/plugins/publish-plugin/connections/" +
                    "{connectionId}/{channel}")
    @TpmDoc(
            description = "Send a message to connected consumers. If content type is binary the body " +
                    "must be a base-64 encoded byte array.",
            path = {
                    @PathParameter(key = "connectionId", description = "Connection Id (0 = all)"),
                    @PathParameter(key = "channel", description = "Session channel (0 = all)")
            },
            requests = @TpmRequest(
                    body = PublishAmqp10Message.class
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
        var messageData = mapper.deserialize(request.getRequestText().toString(), PublishAmqp10Message.class);
        var connectionId = Integer.parseInt(request.getPathParameter("connectionId"));
        var channelId = Integer.parseInt(request.getPathParameter("channel"));

        if (doPublish(messageData, connectionId, channelId) == 0) {
            respondKo(response, "Publish failed");
        } else {
            respondOk(response);
        }
    }

    public int doPublish(PublishAmqp10Message messageData, int connectionId, int channelId) {
        var pInstance = getDescriptor().getProtocolInstance();
        boolean binary = MimeChecker.isBinary(messageData.getContentType(), null);
        byte[] dataToSend = binary
                ? Base64.decode(messageData.getBody())
                : messageData.getBody().getBytes(StandardCharsets.UTF_8);
        var written = 0;
        for (var contextValue : pInstance.getContextsCache().entrySet()) {
            if (connectionId != 0 && !contextValue.getKey().equals(connectionId)) {
                continue;
            }
            if (!(contextValue.getValue() instanceof Amqp10ProtoContext)) {
                continue;
            }
            var context = (Amqp10ProtoContext) contextValue.getValue();
            for (var link : context.getReceiverLinks().values()) {
                if (channelId != 0 && link.getChannel() != channelId) {
                    continue;
                }
                if (messageData.getSource() != null && !messageData.getSource().isEmpty()
                        && !messageData.getSource().equalsIgnoreCase(link.getSource())) {
                    continue;
                }
                try {
                    var deliveryId = context.nextDeliveryId(link.getChannel());
                    var frame = buildTransfer(link.getChannel(), link.getHandle(), deliveryId,
                            messageData.getDeliveryTag(), binary, dataToSend);
                    var raw = new RawFrame(Performatives.TRANSFER, FrameType.AMQP.asByte());
                    raw.setChannel(link.getChannel());
                    raw.setRaw(frame);
                    context.write(raw);
                    written++;
                } catch (Exception e) {
                    log.error("[PUBLISH] cannot inject transfer on link {}", link.getName(), e);
                }
            }
        }
        return written;
    }

    /** Encodes a settled single-frame {@code transfer} carrying one message section. */
    private byte[] buildTransfer(short channel, long handle, long deliveryId, long deliveryTag,
                                 boolean binary, byte[] data) {
        var deliveryTagBytes = new byte[]{
                (byte) (deliveryTag >> 24), (byte) (deliveryTag >> 16),
                (byte) (deliveryTag >> 8), (byte) deliveryTag};
        // transfer fields: handle(0) delivery-id(1) delivery-tag(2) message-format(3) settled(4)
        List<Object> fields = new ArrayList<>();
        fields.add(UnsignedInt.of(handle));
        fields.add(UnsignedInt.of(deliveryId));
        fields.add(new Amqp10Binary(deliveryTagBytes));
        fields.add(UnsignedInt.of(0));   // message-format
        fields.add(Boolean.TRUE);        // settled: no disposition needed

        var body = new BBuffer();
        writer.writeDescribed(body, Performatives.TRANSFER, fields);
        // one message section: data (binary) or amqp-value (text)
        long sectionCode = binary ? Performatives.DATA : Performatives.AMQP_VALUE;
        Object sectionValue = binary ? new Amqp10Binary(data) : new String(data, StandardCharsets.UTF_8);
        writer.writeAny(body, new DescribedType(UnsignedLong.of(sectionCode), sectionValue));

        return Amqp10Frames.frame(channel, FrameType.AMQP.asByte(), body.getAll());
    }

    @HttpMethodFilter(
            pathAddress = "/protocols/{#protocolInstanceId}/plugins/{#plugin}/connections",
            method = "GET", id = "GET /protocols/{#protocolInstanceId}/plugins/{#plugin}/connections")
    public void retrieveConnections(Request request, Response response) {
        var model = new Amqp10Connections();
        model.setConnections(loadConnections());
        model.setInstanceId(getProtocolInstanceId());
        resolversFactory.render("amqp10/publish_plugin/connections.jte", model, response);
    }
}
