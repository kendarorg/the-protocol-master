package org.kendar.plugins;

import org.kendar.plugins.settings.BasicMockPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.ChangeableReference;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MockPlugin<T, K> extends ProtocolPluginDescriptor<T, K,BasicMockPluginSettings> {
    protected final ConcurrentHashMap<Long, AtomicInteger> counters = new ConcurrentHashMap<>();
    protected List<MockStorage> mocks = new ArrayList<>();
    private String mocksDir;


    protected static boolean isTemplateParameter(String tplSeg) {
        return tplSeg.startsWith("${") && tplSeg.endsWith("}") || tplSeg.startsWith("@{") && tplSeg.endsWith("}");
    }

    protected abstract void checkMatching(MockStorage data,
                                          T requestToSimulate,
                                          ChangeableReference<Integer> matchingQuery,
                                          ChangeableReference<Long> foundedIndex);

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, T request, K response) {
        if (!isActive()) return false;
        var matchingQuery = new ChangeableReference<>(0);
        var foundedIndex = new ChangeableReference<>(-1L);
        var withHost = firstCheckOnMainPart(request);
        withHost.forEach(a -> checkMatching(a, request, matchingQuery, foundedIndex));
        if (foundedIndex.get() > 0) {
            var foundedResponse = mocks.stream().filter(a -> a.getIndex() == foundedIndex.get()).findFirst();
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
                        writeOutput(request, response, founded);
                        return true;
                    }
                    return false;
                } else if (founded.getCount() > 0) {
                    founded.setCount(founded.getCount() - 1);
                    writeOutput(request, response, founded);
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
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {

        super.initialize(global, protocol, pluginSetting);
        mocksDir = getSettings().getDataDir();
        loadMocks();
        return this;

    }

    protected void loadMocks() {
        try {
            var mocksPath = Path.of(getMocksDir()).toAbsolutePath();
            mocks = new ArrayList<>();
            var presentAlready = new HashSet<Long>();
            for (var file : mocksPath.toFile().listFiles()) {
                if (file.isFile() && file.getName().endsWith("." + getInstanceId() + ".json")) {
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
