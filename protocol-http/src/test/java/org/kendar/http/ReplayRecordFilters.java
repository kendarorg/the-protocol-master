package org.kendar.http;

import org.junit.jupiter.api.Test;
import org.kendar.events.EventsQueue;
import org.kendar.events.TpmEvent;
import org.kendar.events.WriteItemEvent;
import org.kendar.http.plugins.HttpRecordPlugin;
import org.kendar.http.plugins.HttpRecordPluginSettings;
import org.kendar.http.plugins.HttpReplayPlugin;
import org.kendar.http.plugins.HttpReplayPluginSettings;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.NullStorageRepository;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.Sleeper;

import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReplayRecordFilters {

    private ConcurrentLinkedQueue<TpmEvent> events = new ConcurrentLinkedQueue<>();

    @Test
    void testRecordSites() {
        try {
            events.clear();
            EventsQueue.register("testRecordSites", (e) -> events.add(e), WriteItemEvent.class);
            var rwPlugin = new HttpRecordPlugin() {
                @Override
                public boolean isActive() {
                    return true;
                }
            };
            var settings = new HttpRecordPluginSettings();
            settings.setActive(true);
            settings.getRecordSites().add("test_sites/*");
            settings.getRecordSites().add("www.sara.com");
            settings.getRecordSites().add("@.*microsoft.*");
            var global = new GlobalSettings();
            global.putService("storage", new NullStorageRepository());
            rwPlugin.initialize(global, new HttpProtocolSettings(), settings);

            var pc = new PluginContext("http", null, 0L, null);
            pc.getTags().put("id", 1L);
            var in = new Request();
            in.setMethod("GET");
            in.setPath("/test_sites");
            in.setProtocol("http");


            in.setHost("test_sites");
            rwPlugin.handle(pc, ProtocolPhase.POST_CALL, in, null);
            Sleeper.sleep(10);
            assertTrue(events.size() == 1);
            events.clear();

            in.setHost("www.sara.com");
            rwPlugin.handle(pc, ProtocolPhase.POST_CALL, in, null);
            Sleeper.sleep(10);
            assertTrue(events.size() == 1);
            events.clear();

            in.setHost("www.wetheaver.microsofto.com");
            rwPlugin.handle(pc, ProtocolPhase.POST_CALL, in, null);
            Sleeper.sleep(10);
            assertTrue(events.size() == 1);
            events.clear();


            in.setHost("www.wetheaver.microsof.com");
            rwPlugin.handle(pc, ProtocolPhase.POST_CALL, in, null);
            Sleeper.sleep(10);
            assertTrue(events.size() == 0);
            events.clear();
        } finally {

            EventsQueue.unregister("testRecordSites", WriteItemEvent.class);
        }
    }

    @Test
    void testReplaySites() {
        var matched = new ChangeableReference<Boolean>(false);
        var rwPlugin = new HttpReplayPlugin() {
            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            protected boolean doSend(PluginContext pluginContext, Request in, Response out) {
                matched.set(true);
                return true;
            }
        };
        var settings = new HttpReplayPluginSettings();
        settings.setActive(true);
        settings.getMatchSites().add("test_sites");
        settings.getMatchSites().add("www.sara.com");
        settings.getMatchSites().add("@.*microsoft.*");
        var global = new GlobalSettings();
        global.putService("storage", new NullStorageRepository());

        rwPlugin.initialize(global, new HttpProtocolSettings(), settings);

        var pc = new PluginContext("http", null, 0L, null);
        var in = new Request();
        in.setMethod("GET");
        in.setPath("/test_sites");
        in.setProtocol("http");


        in.setHost("test_sites");
        matched.set(false);
        rwPlugin.handle(pc, ProtocolPhase.PRE_CALL, in, null);
        assertTrue(matched.get());

        in.setHost("www.sara.com");
        matched.set(false);
        rwPlugin.handle(pc, ProtocolPhase.PRE_CALL, in, null);
        assertTrue(matched.get());

        in.setHost("www.wetheaver.microsofto.com");
        matched.set(false);
        rwPlugin.handle(pc, ProtocolPhase.PRE_CALL, in, null);
        assertTrue(matched.get());


        in.setHost("www.wetheaver.microsof.com");
        matched.set(false);
        rwPlugin.handle(pc, ProtocolPhase.PRE_CALL, in, null);
        assertFalse(matched.get());
    }
}
