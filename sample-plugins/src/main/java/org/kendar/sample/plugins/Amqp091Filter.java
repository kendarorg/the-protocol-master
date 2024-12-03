package org.kendar.sample.plugins;

import org.kendar.amqp.v09.messages.methods.channel.ChannelOpen;
import org.kendar.amqp.v09.messages.methods.channel.ChannelOpenOk;
import org.kendar.plugins.base.AlwaysActivePlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.pf4j.Extension;

import java.util.List;

@Extension
public class Amqp091Filter extends ProtocolPluginDescriptorBase<Amqp091FilterSettings> implements AlwaysActivePlugin {


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


    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, ChannelOpen in, ChannelOpenOk out) {
        return false;
    }
}
