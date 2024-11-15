package org.kendar.plugins;

import org.kendar.plugins.settings.BasicMockPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;

import java.util.List;

public abstract class MockPlugin<T, K> extends ProtocolPluginDescriptor<T, K> {
    private String mocksDir;



    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, T in, K out) {
        if(!isActive())return false;
        return false;
    }

    public void setSettings(PluginSettings plugin) {
        super.setSettings(plugin);
        this.mocksDir = ((BasicMockPluginSettings)plugin).getDataDir();
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "mock-plugin";
    }

    @Override
    public void terminate() {

    }

    @Override
    public Class<?> getSettingClass() {
        return BasicMockPluginSettings.class;
    }

    public String getMocksDir() {
        return mocksDir;
    }
}
