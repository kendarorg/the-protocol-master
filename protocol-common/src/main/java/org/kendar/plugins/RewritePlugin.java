package org.kendar.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.plugins.settings.RewritePluginSettings;
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

public abstract class RewritePlugin<T, K, W extends RewritePluginSettings, J> extends ProtocolPluginDescriptorBase<W> {

    private static final Logger log = LoggerFactory.getLogger(RewritePlugin.class);
    private final List<ReplacerItemInstance> replacers = new ArrayList<>();

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
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        var settings = getSettings();
        if (settings.getRewritesFile() == null) return null;
        var path = Path.of(settings.getRewritesFile()).toAbsolutePath();
        if (!path.toFile().exists()) return null;

        try {
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
