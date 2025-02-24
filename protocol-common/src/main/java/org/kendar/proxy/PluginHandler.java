package org.kendar.proxy;


import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PluginHandler {
    private final Class<?> typeIn;
    private final Class<?> typeOut;
    private final Method method;
    private final ProtocolPluginDescriptor target;

    public PluginHandler(ProtocolPluginDescriptor target, Class<?> typeIn, Class<?> typeOut, Method method) {
        this.target = target;

        try {
            this.typeIn = (typeIn);
            this.typeOut = (typeOut);
            this.method = method;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<PluginHandler> of(BasePluginDescriptor plugin, ProtoDescriptor protocol) {
        return of(plugin, "handle", protocol);
    }

    public static List<PluginHandler> of(BasePluginDescriptor plugin, String methodName, ProtoDescriptor protocol) {
        var result = new ArrayList<PluginHandler>();
        ((ProtocolPluginDescriptor) plugin).setProtocolInstance(protocol);
        var clazz = plugin.getClass();
        var handles = Arrays.stream(clazz.getMethods()).filter(m -> m.getName().equalsIgnoreCase(methodName)).toList();
        for (var handle : handles) {
            if (handle.getParameterCount() != 4) continue;
            if (handle.getParameters()[0].getType() != PluginContext.class ||
                    handle.getParameters()[1].getType() != ProtocolPhase.class) continue;
            var inParam = handle.getParameters()[2].getType();
            var outParam = handle.getParameters()[3].getType();
            result.add(new PluginHandler((ProtocolPluginDescriptor) plugin, inParam, outParam, handle));
        }
        return result;
    }

    public ProtocolPluginDescriptor getTarget() {
        return target;
    }

    public String getId() {
        return target.getId();
    }

    public boolean isActive() {
        return target.isActive();
    }

    public void setActive(boolean active) {
        target.setActive(active);
    }

    public boolean handle(PluginContext context, ProtocolPhase phase, Object in, Object out) {
        try {
            if (in != null && typeIn.isAssignableFrom(in.getClass())) {
                if (out != null && typeOut.isAssignableFrom(out.getClass())) {
                    return (boolean) method.invoke(target, context, phase, in, out);
                } else if (out == null) {
                    return (boolean) method.invoke(target, context, phase, in, out);
                }
            } else if (in == null && out != null && typeOut.isAssignableFrom(out.getClass())) {
                return (boolean) method.invoke(target, context, phase, in, out);
            }
            return false;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public String getKey() {
        return typeIn.getName() + "," + typeOut.getName();
    }

    public List<ProtocolPhase> getPhases() {
        return target.getPhases();
    }

    public void setProtocol(ProtoDescriptor protocol) {
        target.setProtocolInstance(protocol);
    }
}

