package org.kendar.plugins.apis;

import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.storage.PluginFileManager;
import org.kendar.ui.MultiTemplateEngine;


public class BasicRestPluginsPluginApis extends ProtocolPluginApiHandlerDefault<BasicRestPluginsPlugin> {


    private final PluginFileManager storage;
    private final MultiTemplateEngine resolversFactory;

    public BasicRestPluginsPluginApis(BasicRestPluginsPlugin descriptor, String id, String instanceId,
                                      PluginFileManager storage, MultiTemplateEngine resolversFactory) {
        super(descriptor, id, instanceId);
        this.storage = storage;
        this.resolversFactory = resolversFactory;
    }
}
