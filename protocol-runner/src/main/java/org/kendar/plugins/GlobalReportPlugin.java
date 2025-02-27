package org.kendar.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.base.BasePluginApiHandler;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.StorageFile;
import org.kendar.storage.StorageFileIndex;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@TpmService
public class GlobalReportPlugin implements GlobalPluginDescriptor {
    private static final Logger log = LoggerFactory.getLogger(GlobalReportPlugin.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<ReportDataEvent> events = new ArrayList<>();
    private final Map<String, Long> counters = new HashMap<>();
    private final StorageRepository repository;
    private final JsonMapper mapper;
    private final SimpleParser simpleParser;
    private boolean active;
    private AtomicInteger counter = new AtomicInteger(0);

    public GlobalReportPlugin(StorageRepository repository, JsonMapper mapper, SimpleParser simpleParser) {
        this.repository = repository;
        this.mapper = mapper;
        this.simpleParser = simpleParser;
    }

    public static String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }

    @Override
    public GlobalPluginDescriptor initialize(GlobalSettings global, PluginSettings pluginSettings) {
        setActive(pluginSettings.isActive());
        EventsQueue.register("GlobalReportPlugin", m -> executor.submit(() -> handleReport(m)), ReportDataEvent.class);
        return this;
    }

    private void handleReport(ReportDataEvent m) {
        if (isActive()) {
            var index = counter.incrementAndGet();
            this.repository.writePluginFile(new StorageFile(new StorageFileIndex("global", getId(),
                    padLeftZeros(index + "", 10) + ".report"), mapper.serialize(m)));
            log.info(m.toString());
            events.add(m);
            var tagId = m.getProtocol() + "." + m.getInstanceId();
            for (var tag : m.getTags().entrySet()) {
                var id = tag.getKey();
                if (id.startsWith("@")) {
                    if (!counters.containsKey(tagId + "." + id)) {
                        counters.put(tagId + "." + id, 0L);
                    }
                    counters.put(tagId + "." + id, counters.get(tagId + "." + id) + (long) tag.getValue());
                }
            }

        }
    }

    @Override
    public BasePluginApiHandler getApiHandler() {
        return new GlobalReportPluginApiHandler(this, repository, simpleParser);
    }

    @Override
    public String getId() {
        return "report-plugin";
    }

    @Override
    public Class<?> getSettingClass() {
        return PluginSettings.class;
    }

    @Override
    public void terminate() {
        EventsQueue.unregister("GlobalReportPlugin", ReportDataEvent.class);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = true;
    }

    @Override
    public BasePluginDescriptor duplicate() {
        try {
            return this.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Should implement clone for " + this.getClass(), e);
        }
    }

    @Override
    public void refreshStatus() {

    }

    public GlobalReport getReport() {
        return new GlobalReport(events, counters);
    }
}
