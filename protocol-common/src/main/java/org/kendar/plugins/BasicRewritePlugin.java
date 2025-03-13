package org.kendar.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.events.EventsQueue;
import org.kendar.events.StorageReloadedEvent;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.plugins.settings.RewritePluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.PluginFileManager;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.ReplacerItem;
import org.kendar.utils.ReplacerItemInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class BasicRewritePlugin<T, K, W extends RewritePluginSettings, J> extends ProtocolPluginDescriptorBase<W> {

    private static final Logger log = LoggerFactory.getLogger(BasicRewritePlugin.class);
    private final List<ReplacerItemInstance> replacers = new ArrayList<>();
    private final StorageRepository repository;
    private PluginFileManager storage;

    public BasicRewritePlugin(JsonMapper mapper, StorageRepository repository) {
        super(mapper);
        this.repository = repository;
        EventsQueue.register(UUID.randomUUID().toString(), (e) -> handleSettingsChanged(), StorageReloadedEvent.class);
    }

    protected abstract Class<?> getIn();

    protected abstract Class<?> getOut();

    @Override
    public String getId() {
        return "rewrite-plugin";
    }

    @Override
    public Class<?> getSettingClass() {
        return RewritePluginSettings.class;
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object request, Object response) {

        if (!isActive()) return false;
        if (replacers.isEmpty()) return false;
        if (request != null && !request.getClass().equals(getIn())) {
            return false;
        }
        if (response != null && !response.getClass().equals(getOut())) {
            return false;
        }
        J toReplace = prepare((T) request, (K) response);
        for (var item : replacers) {
            replaceData(item, toReplace, (T) request, (K) response);
        }
        return false;
    }

    protected abstract J prepare(T request, K response);

    protected abstract void replaceData(ReplacerItemInstance item, J toReplace, T request, K response);

    @Override
    protected boolean handleSettingsChanged() {
        if (getSettings() == null) return false;
        var rewriteFile = storage.readFile( "rewrite");
        if (rewriteFile == null) return false;

        try {
            for (var replacer : mapper.deserialize(rewriteFile, new TypeReference<List<ReplacerItem>>() {
            })) {
                replacers.add(new ReplacerItemInstance(replacer, useTrailing()));
            }
        } catch (Exception e) {
            log.error("Unable to read rewrite file", e);
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        storage = repository.buildPluginFileManager(getInstanceId(),getId());
        if (!handleSettingsChanged()) return null;

        return this;
    }

    protected boolean useTrailing() {
        return false;
    }

    public void setReplacers(List<ReplacerItem> items) {
        for (var replacer : items) {
            replacers.add(new ReplacerItemInstance(replacer, useTrailing()));
        }
    }
}
