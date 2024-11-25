package org.kendar.sample.plugins;

import org.kendar.amqp.v09.messages.methods.channel.ChannelOpen;
import org.kendar.amqp.v09.messages.methods.channel.ChannelOpenOk;
import org.kendar.plugins.AlwaysActivePlugin;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.plugins.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.Extension;

import java.util.List;

@Extension
public class Amqp091Filter extends ProtocolPluginDescriptor<ChannelOpen, ChannelOpenOk> implements AlwaysActivePlugin {


    /**
     * Only PRE_CALL and POST_CALL for things different from http
     *
     * @return
     */
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of();
    }

    @Override
    public String getId() {
        return "sample-amqp";
    }

    @Override
    public String getProtocol() {
        return "amqp091";
    }

    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        super.initialize(global, protocol);
        return this;
    }

    @Override
    public void terminate() {

    }

    @Override
    public Class<?> getSettingClass() {
        return Amqp091FilterSettings.class;
    }

    @Override
    public PluginDescriptor setSettings(GlobalSettings globalSettings, PluginSettings plugin) {
        super.setSettings(globalSettings, plugin);

        return this;
    }

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, ChannelOpen in, ChannelOpenOk out) {
        return false;
    }
}
