package org.kendar.filters;

import org.kendar.amqp.v09.messages.methods.channel.ChannelOpen;
import org.kendar.amqp.v09.messages.methods.channel.ChannelOpenOk;
import org.pf4j.Extension;

import java.util.List;
import java.util.Map;

@Extension
public class Amqp091Filter extends ProtocolPluginDescriptor<ChannelOpen, ChannelOpenOk> implements AlwaysActivePlugin{


    /**
     * Only PRE_CALL and POST_CALL for things different from http
     * @return
     */
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of();
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }

    @Override
    public void initialize(Map<String, Object> section) {

    }

    @Override
    public void terminate() {

    }

    @Override
    public boolean handle(ProtocolPhase phase, ChannelOpen in, ChannelOpenOk out) {
        return false;
    }
}
