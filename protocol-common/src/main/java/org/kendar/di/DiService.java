package org.kendar.di;

import org.kendar.annotations.TpmConstructor;
import org.kendar.annotations.di.TpmNamed;
import org.kendar.annotations.di.TpmPostConstruct;
import org.kendar.annotations.di.TpmService;
import org.kendar.annotations.di.TpmTransient;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class DiService {

    private final HashMap<Type, List<Type>> mappings = new HashMap<>();
    private final HashMap<Type, List<Type>> transientMappings = new HashMap<>();
    private final HashMap<Class<?>,Object> singletons = new HashMap<>();

    public void register(Class<?> clazz, Object instance) {
        singletons.put(clazz, instance);
        bind(instance.getClass(), mappings);
    }

    public void loadPackage(String packageName) {
        Reflections packageReflections = new Reflections(packageName);
        var serviceAnnotations = new HashSet<>(List.of(TpmService.class));
        serviceAnnotations.stream()
                .map(packageReflections::getTypesAnnotatedWith)
                .flatMap(Set::stream)
                .forEach((t) -> {
                    this.bind(t,mappings);
                });
        var transientAnnotations = new HashSet<>(List.of(TpmTransient.class));
        transientAnnotations.stream()
                .map(packageReflections::getTypesAnnotatedWith)
                .flatMap(Set::stream)
                .forEach((t) -> {
                    this.bind(t,transientMappings);
                });

    }

    public void bind(Class<?> t){
        this.bind(t,mappings);
    }


    public void bindTransient(Class<?> t){
        this.bind(t,transientMappings);
    }

    private void bind(Class<?> t, HashMap<Type, List<Type>> mappings) {
        List<Type> interfacesFound = new ArrayList<>();

        getAllInterfaces(t, interfacesFound);
        var implOfInt = new ArrayList<Type>();
        implOfInt.add(t);
        mappings.put(t, implOfInt);
        for (Type i : interfacesFound) {
            if (!mappings.containsKey(i)) {
                mappings.put(i, new ArrayList<>());
            }
            if(!mappings.get(i).contains(t)){
                mappings.get(i).add(t);
            }
        }

    }

    private void getAllInterfaces(Type type,
                                  List<Type> interfacesFound) {
        while (type != null) {
            Class<?> clazz;
            if (type instanceof Class<?>) {
                clazz = (Class<?>) type;
            } else if (type instanceof ParameterizedType) {
                clazz = (Class<?>) ((ParameterizedType) type).getRawType();
            } else {
                throw new RuntimeException("Type " + type + " not supported");
            }

            Class<?>[] interfaces = clazz.getInterfaces();

            for (int i = 0; i < interfaces.length; i++) {
                if (!interfacesFound.contains(interfaces[i])) {
                    interfacesFound.add(interfaces[i]);
                    getAllInterfaces(interfaces[i], interfacesFound);
                }
            }
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            for (int i = 0; i < genericInterfaces.length; i++) {
                if (genericInterfaces[i] instanceof ParameterizedType) {
                    var parameterizedType = (ParameterizedType) genericInterfaces[i];
                    interfacesFound.add(parameterizedType);
                    getAllInterfaces(parameterizedType, interfacesFound);
                }
            }
            clazz = clazz.getSuperclass();
            if (clazz != null && clazz != Object.class) {
                interfacesFound.add(clazz);
            }
            if (clazz == Object.class) {
                return;
            }
            type = clazz;
        }
    }

    public <T> T getInstance(Class<T> clazz,String ...tags) {
        var result = getInstances(clazz,tags);
        if (result == null || result.isEmpty()) return null;
        return result.get(0);
    }

    private Object getInstance(Type type,String ...tags) {
        var result = getInstances(type,tags);
        if (result == null || result.isEmpty()) return null;
        return result.get(0);
    }

    private List<Object> getInstances(Type type,String...tags) {
        var data = mappings.get(type);
        var transi=false;
        if (data == null) {
            data = transientMappings.get(type);
            transi=true;
            if(data == null) {
                return new ArrayList<>();
            }
        }
        var result = new ArrayList<Object>();
        for (var i : data) {
            if(tags.length>0) {
                if(((Class<?>)i).getAnnotation(TpmService.class)==null){
                    continue;
                }
                var iTags = ((Class<?>)i).getAnnotation(TpmService.class).tags();
                if(iTags.length!=tags.length) {
                    continue;
                }
                if(!Arrays.asList(iTags).containsAll(Arrays.asList(tags))){
                    continue;
                }
            }
            result.add(this.createInstance((Class<?>) i,transi));
        }
        return result;
    }

    public <T> List<T> getInstances(Class<T> clazz,String ... tags) {
        var data = mappings.get(clazz);
        var transi=false;
        if (data == null) {
            data = transientMappings.get(clazz);
            transi=true;
            if(data == null) {
                return new ArrayList<>();
            }
        }
        var result = new ArrayList<T>();
        for (var i : data) {
            if(tags.length>0) {
                var iTags = ((Class<?>)i).getAnnotation(TpmService.class).tags();
                if(iTags.length!=tags.length) {
                    continue;
                }
                if(!Arrays.asList(iTags).containsAll(Arrays.asList(tags))){
                    continue;
                }
            }
            result.add((T) this.createInstance((Class<?>) i,transi));
        }
        return result;
    }

    private Object createInstance(Class<?> clazz, boolean transi) {
        try {
            var constructors = clazz.getConstructors();
            var clazzNamed = clazz.getAnnotation(TpmNamed.class);
            Constructor constructor = null;
            if (constructors.length == 1) {
                constructor = constructors[0];
            }else{
                for(var constr:constructors){
                    if(null!=constr.getAnnotation(TpmConstructor.class)){
                        if(constructor!=null){
                            throw new RuntimeException("Duplicate TpmConstructor constructor found");
                        }
                        constructor = constr;
                    }
                }
            }

            if(constructor==null){
                throw new RuntimeException("No TpmConstructor found");
            }
            if (constructor.getParameterCount() == 0) {
                if(transi){
                    return postConstruct(constructor.newInstance());
                }
                if(!singletons.containsKey(clazz)){
                    singletons.put(clazz, postConstruct(constructor.newInstance()));
                }
                return singletons.get(clazz);
            }
            var parameters = constructor.getParameters();
            var values = new Object[parameters.length];
            for (var i = 0; i < parameters.length; i++) {
                var parameter = parameters[i];
                var parameterAnnotation = parameter.getAnnotation(TpmNamed.class);
                var tags = new String[]{};
                if(parameterAnnotation!=null){
                   tags=parameterAnnotation.tags();
                }
                if (List.class.isAssignableFrom(parameter.getType())) {
                    var args = ((ParameterizedType) parameter.getParameterizedType()).getActualTypeArguments();
                    if (args.length == 1) {
                        var arg = args[0];
                        values[i] = getInstances(arg,tags);
                    }
                } else {
                    var parametrizedType = parameter.getParameterizedType();
                    if (parametrizedType instanceof ParameterizedType) {
                        values[i] = getInstance(parametrizedType,tags);
                    } else {
                        values[i] = getInstance(parameter.getType(),tags);
                    }
                }
            }
            if(transi){
                return postConstruct(constructor.newInstance(values));
            }
            if(!singletons.containsKey(clazz)){
                singletons.put(clazz, postConstruct(constructor.newInstance(values)));
            }
            return singletons.get(clazz);

        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate " + clazz, e);
        }
    }

    private Object postConstruct(Object o) {
        var ms = o.getClass().getMethods();
        for(var m:ms){
            if(m.getAnnotation(TpmPostConstruct.class)!=null){
                try {
                    m.invoke(o);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }
        return o;
    }


}
