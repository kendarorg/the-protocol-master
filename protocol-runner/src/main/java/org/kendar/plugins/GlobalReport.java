package org.kendar.plugins;

import org.kendar.events.ReportDataEvent;

import java.util.List;
import java.util.Map;

public class GlobalReport {
    private  List<ReportDataEvent> events;
    private  Map<String, Long> counters;

    public GlobalReport() {
    }

    public List<ReportDataEvent> getEvents() {
        return events;
    }

    public void setEvents(List<ReportDataEvent> events) {
        this.events = events;
    }

    public Map<String, Long> getCounters() {
        return counters;
    }

    public void setCounters(Map<String, Long> counters) {
        this.counters = counters;
    }

    public GlobalReport(List<ReportDataEvent> events, Map<String, Long> counters) {

        this.events = events;
        this.counters = counters;
    }
}
