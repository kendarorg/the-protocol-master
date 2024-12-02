package org.kendar.proxy;


import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PluginHandler {
    private final Class<?> typeIn;
    private final Class<?> typeOut;
    private final Method method;
    private final Object target;

    public PluginHandler(Object target, Class<?> typeIn, Class<?> typeOut, Method method) {
        this.target = target;

        try {
            this.typeIn = (typeIn);
            this.typeOut =(typeOut);
            this.method = method;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PluginHandler(ProtocolPluginDescriptor plugin, Class<?> parameterType, Class<?> parameterType1) {

    }

    boolean handle(PluginContext context, ProtocolPhase phase, Object in, Object out) {
        try {
            if (in != null && typeIn.isAssignableFrom(in.getClass())) {
                if (out != null && typeOut.isAssignableFrom(out.getClass())) {
                    return  (boolean)method.invoke(target, context, phase, in, out);
                }else if(out == null){
                    return  (boolean)method.invoke(target, context, phase, in, out);
                }
            }
            return false;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
