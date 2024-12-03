package org.kendar.plugins;

import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.base.BasePluginApiHandler;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GlobalReportPlugin implements GlobalPluginDescriptor {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean active;

    @Override
    public GlobalPluginDescriptor initialize(GlobalSettings global, PluginSettings pluginSettings) {
        setActive(pluginSettings.isActive());
        EventsQueue.register("GlobalReportPlugin", m->executor.submit(()-> handleReport(m)), ReportDataEvent.class);
        return this;
    }

    private final List<ReportDataEvent> events = new ArrayList<>();
    private final Map<String,Long> counters = new HashMap<>();


    private void handleReport(ReportDataEvent m) {
        if(isActive()){
            events.add(m);
            var tagId = m.getProtocol()+"."+m.getInstanceId();
            for(var tag:m.getTags().entrySet()){
                var id = tag.getKey();
                if(id.startsWith("@")){
                    if(!counters.containsKey(tagId+"."+id)){
                        counters.put(tagId+"."+id, 0L);
                    }
                    counters.put(tagId+"."+id, counters.get(tagId+"."+id)+(long)tag.getValue());
                }
            }

        }
    }

    @Override
    public BasePluginApiHandler getApiHandler() {
        return new GlobalReportPluginApiHandler(this);
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
        return null;
    }

    @Override
    public void refreshStatus() {

    }

    public GlobalReport getReport() {
        return new GlobalReport(events,counters);
    }
}
