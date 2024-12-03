package org.kendar.utils;

import org.junit.jupiter.api.Test;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;

import java.util.List;

public class ExtractTypeTest {
    @Test
    void testExtractType() {
        var target = new ProtocolPluginDescriptorBase<TestPluginSettings>() {
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

            public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
                return false;
            }
        };
        var type = target.getSettingClass();
        System.out.println(type);
    }
}
