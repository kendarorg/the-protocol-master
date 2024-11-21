package org.kendar.http;

import org.junit.jupiter.api.Test;
import org.kendar.http.plugins.HttpRecordPluginSettings;
import org.kendar.http.plugins.HttpRecordingPlugin;
import org.kendar.http.plugins.HttpReplayPluginSettings;
import org.kendar.http.plugins.HttpReplayingPlugin;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.utils.ChangeableReference;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReplayRecordFilters {
    @Test
    void testRecordSites(){
        var matched = new ChangeableReference<Boolean>(false);
        var rwPlugin = new HttpRecordingPlugin(){
            @Override
            public boolean isActive(){return true;}
            @Override
            protected void postCall(PluginContext pluginContext, Object in, Object out) {
                matched.set(true);
            }
        };
        var settings = new HttpRecordPluginSettings();
        settings.setActive(true);
        settings.getRecordSites().add("test_sites");
        settings.getRecordSites().add("www.sara.com");
        settings.getRecordSites().add(".*microsoft.*");
        rwPlugin.setSettings(settings);

        var pc = new PluginContext("http", null,0L,null);
        var in = new Request();
        in.setMethod("GET");
        in.setPath("/test_sites");
        in.setProtocol("http");


        in.setHost("test_sites");
        matched.set(false);
        rwPlugin.handle(pc,ProtocolPhase.POST_CALL,in,null);
        assertTrue(matched.get());

        in.setHost("www.sara.com");
        matched.set(false);
        rwPlugin.handle(pc,ProtocolPhase.POST_CALL,in,null);
        assertTrue(matched.get());

        in.setHost("www.wetheaver.microsofto.com");
        matched.set(false);
        rwPlugin.handle(pc,ProtocolPhase.POST_CALL,in,null);
        assertTrue(matched.get());


        in.setHost("www.wetheaver.microsof.com");
        matched.set(false);
        rwPlugin.handle(pc,ProtocolPhase.POST_CALL,in,null);
        assertFalse(matched.get());
    }

    @Test
    void testReplaySites(){
        var matched = new ChangeableReference<Boolean>(false);
        var rwPlugin = new HttpReplayingPlugin(){
            @Override
            public boolean isActive(){return true;}
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
        settings.getMatchSites().add(".*microsoft.*");
        rwPlugin.setSettings(settings);

        var pc = new PluginContext("http", null,0L,null);
        var in = new Request();
        in.setMethod("GET");
        in.setPath("/test_sites");
        in.setProtocol("http");


        in.setHost("test_sites");
        matched.set(false);
        rwPlugin.handle(pc,ProtocolPhase.PRE_CALL,in,null);
        assertTrue(matched.get());

        in.setHost("www.sara.com");
        matched.set(false);
        rwPlugin.handle(pc,ProtocolPhase.PRE_CALL,in,null);
        assertTrue(matched.get());

        in.setHost("www.wetheaver.microsofto.com");
        matched.set(false);
        rwPlugin.handle(pc,ProtocolPhase.PRE_CALL,in,null);
        assertTrue(matched.get());


        in.setHost("www.wetheaver.microsof.com");
        matched.set(false);
        rwPlugin.handle(pc,ProtocolPhase.PRE_CALL,in,null);
        assertFalse(matched.get());
    }
}
