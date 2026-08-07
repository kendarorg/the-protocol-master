package org.kendar.amqp.v10.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicReportPlugin;
import org.kendar.settings.PluginSettings;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

/**
 * Reports AMQP 1.0 activity. Minimal for now — the frame-level semantic reporting
 * (per-performative CONNECT/SEND/RECEIVE events) that the v09 plugin does needs the
 * M2 codec wired into the frame states; the base plugin still records generic
 * report data.
 */
@Extension
@TpmService(tags = "amqp10")
public class Amqp10ReportPlugin extends BasicReportPlugin<PluginSettings> {
    public Amqp10ReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "amqp10";
    }
}
