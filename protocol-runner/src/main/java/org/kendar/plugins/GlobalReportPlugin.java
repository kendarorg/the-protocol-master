package org.kendar.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.events.*;
import org.kendar.http.events.SSLAddHostEvent;
import org.kendar.plugins.apis.GlobalReportPluginApiHandler;
import org.kendar.plugins.base.BasePluginApiHandler;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.PluginFileManager;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@TpmService
public class GlobalReportPlugin implements GlobalPluginDescriptor {
    private static final Logger log = LoggerFactory.getLogger(GlobalReportPlugin.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    //private final List<ReportDataEvent> events = new ArrayList<>();
    private final Map<String, Long> counters = new HashMap<>();
    private final StorageRepository repository;
    private final JsonMapper mapper;
    private final SimpleParser simpleParser;
    private final MultiTemplateEngine resolversFactory;
    private final AtomicInteger counter = new AtomicInteger(0);
    private boolean active;
    private PluginSettings settings;
    private PluginFileManager storage;

    public GlobalReportPlugin(StorageRepository repository, JsonMapper mapper, SimpleParser simpleParser, MultiTemplateEngine resolversFactory) {
        this.repository = repository;
        this.mapper = mapper;
        this.simpleParser = simpleParser;
        this.resolversFactory = resolversFactory;
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

        storage = repository.buildPluginFileManager("global", getId());
        storage.listFiles().forEach(file -> {
            var spl = file.split("\\.");
            var foundCounter = Long.parseLong(spl[0]);
            counter.set((int) foundCounter);
        });
        setActive(pluginSettings.isActive());
        setSettings(pluginSettings);
        EventsQueue.register("GlobalReportPlugin", m -> executor.submit(() -> handleReport(m)), ReportDataEvent.class);
        EventsQueue.register("GlobalReportPlugin", m -> executor.submit(() -> handleReport(m)), RecordStatusEvent.class);
        EventsQueue.register("GlobalReportPlugin", m -> executor.submit(() -> handleReport(m)), ReplayStatusEvent.class);
        EventsQueue.register("GlobalReportPlugin", m -> executor.submit(() -> handleReport(m)), StorageReloadedEvent.class);
        EventsQueue.register("GlobalReportPlugin", m -> executor.submit(() -> handleReport(m)), SSLAddHostEvent.class);
        EventsQueue.register("GlobalReportPlugin", m -> executor.submit(() -> handleReport(m)), TerminateEvent.class);
        return this;
    }

    private void handleReport(TerminateEvent m) {
        handleReport(new ReportDataEvent("global", "global", "terminate", -1, new Date().getTime(), 0, Map.of()));
    }

    private void handleReport(SSLAddHostEvent m) {
        handleReport(new ReportDataEvent(m.getInstanceId(), "http", "addedhost:" + m.getHost(), -1, new Date().getTime(), 0, Map.of()));
    }

    private void handleReport(StorageReloadedEvent m) {
        if (!isActive()) return;
        Map<String, Object> tags = new HashMap<>();
        if (m.getSettings() != null) {
            tags.put("addedSettings", "true");
        }
        handleReport(new ReportDataEvent("global", "global", "storage-reloaded", -1, new Date().getTime(), 0, tags));
    }

    private void handleReport(ReplayStatusEvent m) {
        if (!isActive()) return;
        handleReport(new ReportDataEvent(m.getInstanceId(), m.getProtocol(), m.isReplaying() ? "start-replaying" : "stop-replaying", -1, new Date().getTime(), 0, Map.of()));
    }

    private void handleReport(RecordStatusEvent m) {
        if (!isActive()) return;
        handleReport(new ReportDataEvent(m.getInstanceId(), m.getProtocol(), m.isRecording() ? "start-recording" : "stop-recording", -1, new Date().getTime(), 0, Map.of()));
    }

    private void handleReport(ReportDataEvent m) {
        if (isActive()) {
            var index = counter.incrementAndGet();
            this.storage.writeFile(padLeftZeros(index + "", 10) + ".report", mapper.serialize(m));
            log.info(m.toString());
            //events.add(m);
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
        return new GlobalReportPluginApiHandler(this, repository, simpleParser, resolversFactory);
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
    public void refreshStatus() {

    }

    public GlobalReport getReport() {
        var events = new ArrayList<ReportDataEvent>();
        List<String> listFiles = storage.listFiles();
        for (String item : listFiles) {
            var rde = mapper.deserialize(storage.readFile(item), ReportDataEvent.class);
            events.add(rde);
        }
        return new GlobalReport(events, counters);
    }

    public PluginSettings getSettings() {
        return settings;
    }

    public void setSettings(PluginSettings settings) {
        this.settings = settings;
    }
}
