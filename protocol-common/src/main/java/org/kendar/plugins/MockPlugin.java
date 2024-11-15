package org.kendar.plugins;

import org.kendar.plugins.settings.BasicMockPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MockPlugin<T, K> extends ProtocolPluginDescriptor<T, K> {
    protected final ConcurrentHashMap<Long, AtomicInteger> counters = new ConcurrentHashMap<>();
    protected List<MockStorage> mocks = new ArrayList<>();
    private String mocksDir;

    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {

        super.initialize(global, protocol);
        var thisPlugin = (BasicMockPluginSettings) protocol.getPlugins().get(getId());
        if (thisPlugin != null) {
            mocksDir = thisPlugin.getDataDir();
        }
        return this;

    }

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, T in, K out) {
        if (!isActive()) return false;
        return false;
    }

    public void setSettings(PluginSettings plugin) {
        super.setSettings(plugin);
        this.mocksDir = ((BasicMockPluginSettings) plugin).getDataDir();
        loadMocks();
    }

    protected void loadMocks() {
        try {
            var mocksPath = Path.of(getMocksDir()).toAbsolutePath();
            mocks = new ArrayList<>();
            var presentAlready = new HashSet<Long>();
            for (var file : mocksPath.toFile().listFiles()) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    var si = mapper.deserialize(Files.readString(file.toPath()), MockStorage.class);
                    if (presentAlready.contains(si.getIndex())) throw new RuntimeException(
                            "Duplicate id " + si.getIndex() + " found in " + file.getName());
                    presentAlready.add(si.getIndex());
                    mocks.add(si);
                    counters.put(si.getIndex(), new AtomicInteger(0));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    protected void handleActivation(boolean active) {
        if (active != this.isActive()) {
            counters.clear();
            if (active) {
                loadMocks();
            } else {
                mocks.clear();
            }
        }
        super.handleActivation(active);
    }

}
