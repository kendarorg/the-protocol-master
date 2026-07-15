package org.kendar.amqp.v10.plugins;

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
        return data;
    }
}
