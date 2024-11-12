package org.kendar.filters;

import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.ExtensionPoint;

import java.util.List;

public interface PluginDescriptor extends ExtensionPoint {
    List<ProtocolPhase> getPhases();

    String getId();

    String getProtocol();

    PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol);

    void terminate();

    PluginDescriptor clone();

    Class<?> getSettingClass();

    void setSettings(PluginSettings plugin);

    boolean isActive();

    void setActive(boolean active);
}
