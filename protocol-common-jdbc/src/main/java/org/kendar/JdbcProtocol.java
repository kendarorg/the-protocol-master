package org.kendar;

import org.kendar.di.annotations.TpmNamed;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.sql.jdbc.JdbcProxy;

import java.util.List;

public abstract class JdbcProtocol extends NetworkProtoDescriptor {
    public JdbcProtocol(GlobalSettings ini, ProtocolSettings settings, JdbcProxy proxy,
                        List<BasePluginDescriptor> plugins) {
        super(ini, settings, proxy, plugins);
    }

    public JdbcProtocol(int port){

    }

    @Override
    public boolean isLateConnect(){
        return true;
    }
}
