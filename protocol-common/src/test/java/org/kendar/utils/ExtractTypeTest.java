package org.kendar.utils;

import org.junit.jupiter.api.Test;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.plugins.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;

import java.util.List;

public class ExtractTypeTest {
    @Test
    void testExtractType() {
        var target = new ProtocolPluginDescriptor<Object,Object,TestPluginSettings>(){
            @Override
            public List<ProtocolPhase> getPhases() {
                return List.of();
            }

            @Override
            public String getId() {
                return "";
            }

            @Override
            public String getProtocol() {
                return "";
            }

            @Override
            public void terminate() {

            }

            @Override
            public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
                return false;
            }
        };
        var type = target.getSettingClass();
        System.out.println(type);
    }
}
