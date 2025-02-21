package org.kendar.plugins;

import org.kendar.events.EventsQueue;
import org.kendar.events.StorageReloadedEvent;
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
import org.kendar.storage.StorageFile;
import org.kendar.storage.StorageFileIndex;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.JsonMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MockPlugin<T, K> extends ProtocolPluginDescriptorBase<BasicMockPluginSettings> {
    protected final ConcurrentHashMap<Long, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final StorageRepository repository;
    protected Map<String, MockStorage> mocks = new HashMap<>();


    public MockPlugin(JsonMapper mapper, StorageRepository repository) {
        super(mapper);
        this.repository = repository;
        EventsQueue.register(UUID.randomUUID().toString(), (e) -> handleSettingsChanged(), StorageReloadedEvent.class);
    }

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
    protected boolean handleSettingsChanged() {
        if (getSettings() == null) return false;
        loadMocks();
        return true;
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {

        super.initialize(global, protocol, pluginSetting);
        if (!handleSettingsChanged()) return null;

        return this;

    }

    @Override
    public Class<?> getSettingClass() {
        return BasicMockPluginSettings.class;
    }

    protected void loadMocks() {
        try {

            mocks = new HashMap<>();
            var presentAlready = new HashSet<Long>();
            for (var file : repository.listPluginFiles(getInstanceId(), getId())) {

                var si = mapper.deserialize(repository.readPluginFile(file).getContent(), MockStorage.class);
                if (presentAlready.contains(si.getIndex())) throw new RuntimeException(
                        "Duplicate id " + si.getIndex() + " found in " + file.getIndex());
                presentAlready.add(si.getIndex());
                mocks.put(file.getIndex(), si);
                counters.put(si.getIndex(), new AtomicInteger(0));
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
    protected List<ProtocolPluginApiHandler> buildApiHandler() {
        return List.of(new BaseMockPluginApis(this, getId(), getInstanceId()));
    }

    public void putMock(String mockfile, MockStorage inputObject) {
        getMocks().put(mockfile, inputObject);
        var serialized = mapper.serialize(inputObject);
        repository.writePluginFile(new StorageFile(new StorageFileIndex(getInstanceId(), getId(), mockfile), serialized));
    }

    public void delMock(String mockfile) {
        getMocks().remove(mockfile);
        repository.delPluginFile(new StorageFileIndex(getInstanceId(), getId(), mockfile));
    }
}
