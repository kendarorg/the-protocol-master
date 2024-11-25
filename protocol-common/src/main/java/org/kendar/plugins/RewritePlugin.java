package org.kendar.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.ReplacerItem;
import org.kendar.utils.ReplacerItemInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class RewritePlugin<T, K, J> extends ProtocolPluginDescriptor<T, K> {

    private static final Logger log = LoggerFactory.getLogger(RewritePlugin.class);
    private final List<ReplacerItemInstance> replacers = new ArrayList<>();

    @Override
    public String getId() {
        return "rewrite-plugin";
    }

    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        super.initialize(global, protocol);
        return this;
    }

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, T request, K response) {
        if (!isActive()) return false;
        if (replacers.isEmpty()) return false;
        J toReplace = prepare(request, response);
        for (var item : replacers) {
            replaceData(item, toReplace, request, response);
        }
        return false;
    }

    protected abstract J prepare(T request, K response);

    protected abstract void replaceData(ReplacerItemInstance item, J toReplace, T request, K response);

    @Override
    public void terminate() {

    }

    @Override
    public Class<?> getSettingClass() {
        return RewritePluginSettings.class;
    }

    @Override
    public PluginDescriptor setSettings(GlobalSettings globalSettings, PluginSettings plugin) {
        var settings = (RewritePluginSettings) plugin;
        try {
            super.setSettings(globalSettings, plugin);

            if (settings.getRewritesFile() == null) return null;
            var path = Path.of(settings.getRewritesFile()).toAbsolutePath();
            if (!path.toFile().exists()) return null;

            for (var replacer : mapper.deserialize(Files.readString(path), new TypeReference<List<ReplacerItem>>() {
            })) {
                replacers.add(new ReplacerItemInstance(replacer, useTrailing()));
            }
        } catch (Exception e) {
            log.error("Unable to read rewrite file {}", settings.getRewritesFile(), e);
            throw new RuntimeException(e);
        }
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
