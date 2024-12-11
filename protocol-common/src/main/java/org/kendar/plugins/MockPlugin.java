package org.kendar.plugins;

import org.kendar.plugins.apis.BaseMockPluginApis;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginApiHandler;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.plugins.settings.BasicMockPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.ChangeableReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MockPlugin<T, K> extends ProtocolPluginDescriptorBase<BasicMockPluginSettings> {
    protected final ConcurrentHashMap<Long, AtomicInteger> counters = new ConcurrentHashMap<>();
    protected Map<String, MockStorage> mocks = new HashMap<>();
    private String mocksDir;

    protected static boolean isTemplateParameter(String tplSeg) {
        return tplSeg.startsWith("${") && tplSeg.endsWith("}") || tplSeg.startsWith("@{") && tplSeg.endsWith("}");
    }

    public Map<String, MockStorage> getMocks() {
        return mocks;
    }

    protected abstract Class<?> getIn();

    protected abstract Class<?> getOut();

    protected abstract void checkMatching(MockStorage data,
                                          T requestToSimulate,
                                          ChangeableReference<Integer> matchingQuery,
                                          ChangeableReference<Long> foundedIndex);

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object request, Object response) {
        if (!isActive()) return false;
        if (request != null && !request.getClass().equals(getIn())) {
            return false;
        }
        if (response != null && !response.getClass().equals(getOut())) {
            return false;
        }
        var matchingQuery = new ChangeableReference<>(0);
        var foundedIndex = new ChangeableReference<>(-1L);
        var withHost = firstCheckOnMainPart((T) request);
        withHost.forEach(a -> checkMatching(a, (T) request, matchingQuery, foundedIndex));
        if (foundedIndex.get() > 0) {
            var foundedResponse = mocks.values().stream().filter(a -> a.getIndex() == foundedIndex.get()).findFirst();
            if (foundedResponse.isPresent()) {
                var founded = foundedResponse.get();

                counters.get(founded.getIndex()).getAndIncrement();
                if (founded.getNthRequest() > 0) {
                    var isNth = counters.get(founded.getIndex()).get() == founded.getNthRequest();
                    if (isNth) {
                        founded.setNthRequest(-1);
                        if (founded.getCount() > 0) {
                            founded.setCount(founded.getCount() - 1);
                        }
                        writeOutput((T) request, (K) response, founded);
                        return true;
                    }
                    return false;
                } else if (founded.getCount() > 0) {
                    founded.setCount(founded.getCount() - 1);
                    writeOutput((T) request, (K) response, founded);
                    return true;
                }
            }
        }
        return false;
    }

    protected abstract void writeOutput(T request, K response,
                                        MockStorage founded);

    protected abstract List<MockStorage> firstCheckOnMainPart(T request);

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {

        super.initialize(global, protocol, pluginSetting);
        if (getSettings().getDataDir() == null) {
            return null;
        }
        mocksDir = getSettings().getDataDir();
        if (!Files.exists(Path.of(mocksDir).toAbsolutePath())) {
            try {
                Files.createDirectories(Path.of(mocksDir).toAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        loadMocks();
        return this;

    }

    @Override
    public Class<?> getSettingClass() {
        return BasicMockPluginSettings.class;
    }

    protected void loadMocks() {
        try {
            var mocksPath = Path.of(getMocksDir()).toAbsolutePath();
            mocks = new HashMap<>();
            var presentAlready = new HashSet<Long>();
            for (var file : mocksPath.toFile().listFiles()) {
                if (file.isFile() && file.getName().endsWith("." + getInstanceId() + ".json")) {
                    var si = mapper.deserialize(Files.readString(file.toPath()), MockStorage.class);
                    if (presentAlready.contains(si.getIndex())) throw new RuntimeException(
                            "Duplicate id " + si.getIndex() + " found in " + file.getName());
                    presentAlready.add(si.getIndex());
                    mocks.put(file.getName(), si);
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

    @Override
    protected ProtocolPluginApiHandler buildApiHandler() {
        return new BaseMockPluginApis(this, getId(), getInstanceId());
    }
}
