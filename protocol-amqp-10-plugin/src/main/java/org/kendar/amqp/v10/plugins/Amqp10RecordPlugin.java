package org.kendar.amqp.v10.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRecordPlugin;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;
import org.pf4j.Extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records AMQP 1.0 traffic. The proxy relays raw frames ({@code RawFrame}); the
 * handshake performatives that must not be replayed verbatim (headers, SASL,
 * open/close negotiation) are filtered by descriptor via {@link #shouldNotSave}.
 */
@Extension
@TpmService(tags = "amqp10")
public class Amqp10RecordPlugin extends BasicRecordPlugin<BasicAysncRecordPluginSettings> {

    // Frame kinds that are connection setup/teardown and must not be replayed.
    private static final List<String> toAvoid = List.of("byte[]");

    public Amqp10RecordPlugin(JsonMapper mapper, StorageRepository storage,
                              MultiTemplateEngine resolversFactory, SimpleParser parser) {
        super(mapper, storage, resolversFactory, parser);
    }

    @Override
    public Class<?> getSettingClass() {
        return BasicAysncRecordPluginSettings.class;
    }

    @Override
    public String getProtocol() {
        return "amqp10";
    }

    @Override
    protected boolean shouldNotSave(Object in, Object out, CompactLine cl) {
        if (cl == null || cl.getTags() == null) {
            return false;
        }
        var input = cl.getTags().get("input");
        var output = cl.getTags().get("output");
        // NB: List.of(...).contains(null) throws, so null-check first.
        return (input != null && toAvoid.contains(input))
                || (output != null && toAvoid.contains(output));
    }

    @Override
    public Map<String, String> buildTag(StorageItem item) {
        var data = new HashMap<String, String>();
        data.put("input", item.getInput() != null
                ? (item.getInputType().equalsIgnoreCase("BBuffer") ? "byte[]" : item.getInputType()) : null);
        data.put("output", item.getOutput() != null
                ? (item.getOutputType().equalsIgnoreCase("BBuffer") ? "byte[]" : item.getOutputType()) : null);
        // Informational tags for the scenario index (matching is score-based, so
        // extra tags never break replay of older recordings).
        var decoded = decodedOf(item.getInput());
        if (decoded == null) {
            decoded = decodedOf(item.getOutput());
        }
        if (decoded != null) {
            var performative = decoded.get("performative");
            if (performative != null) {
                data.put("performative", performative.asText());
            }
            var address = attachAddress(decoded);
            if (address != null) {
                data.put("address", address);
            }
        }
        return data;
    }

    /** The {@code decoded} node of a serialized frame (see {@code Amqp10BaseFrame#getDecoded}). */
    private static JsonNode decodedOf(Object serialized) {
        if (!(serialized instanceof JsonNode)) {
            return null;
        }
        var decoded = ((JsonNode) serialized).get("decoded");
        return decoded == null || decoded.isNull() ? null : decoded;
    }

    /** The source/target address of a decoded attach, or {@code null}. */
    private static String attachAddress(JsonNode decoded) {
        var performative = decoded.get("performative");
        if (performative == null || !"attach".equals(performative.asText())) {
            return null;
        }
        for (var end : new String[]{"source", "target"}) {
            var address = decoded.at("/fields/" + end + "/fields/address");
            if (address.isTextual()) {
                return address.asText();
            }
        }
        return null;
    }
}
