package org.kendar.proxy;


import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PluginHandler {
    private  Class<?> typeIn;
    private  Class<?> typeOut;
    private  Method method;
    private  ProtocolPluginDescriptor target;

    public static List<PluginHandler> of(ProtocolPluginDescriptor plugin){
        return of(plugin,"handle");
    }
    public static List<PluginHandler> of(ProtocolPluginDescriptor plugin,String methodName){
        var result = new ArrayList<PluginHandler>();
        var clazz = plugin.getClass();
        var handles = Arrays.stream(clazz.getMethods()).filter(m -> m.getName().equalsIgnoreCase(methodName)).collect(Collectors.toList());
        for(var handle:handles) {
            if (handle.getParameterCount() != 4) continue;
            if (handle.getParameters()[0].getType() != PluginContext.class ||
                    handle.getParameters()[1].getType() != ProtocolPhase.class) continue;
            var inParam = handle.getParameters()[2].getType();
            var outParam = handle.getParameters()[3].getType();
            result.add(new PluginHandler(plugin,inParam,outParam,handle));
        }
        return result;
    }

    public PluginHandler(ProtocolPluginDescriptor target, Class<?> typeIn, Class<?> typeOut, Method method) {
        this.target = target;

        try {
            this.typeIn = (typeIn);
            this.typeOut =(typeOut);
            this.method = method;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ProtocolPluginDescriptor getTarget() {
        return target;
    }

    public String getId(){
        return target.getId();
    }

    public void setActive(boolean active){
        target.setActive(active);
    }

    public boolean isActive(){
        return target.isActive();
    }

    public boolean handle(PluginContext context, ProtocolPhase phase, Object in, Object out) {
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

    public String getKey() {
        return typeIn.getName()+","+typeOut.getName();
    }

    public List<ProtocolPhase> getPhases(){
        return target.getPhases();
    }
}
