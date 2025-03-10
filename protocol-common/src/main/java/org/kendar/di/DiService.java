package org.kendar.di;

import org.kendar.di.annotations.*;
import org.kendar.utils.TimerInstance;
import org.kendar.utils.TimerService;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DiService {
    private static final Logger log = LoggerFactory.getLogger(DiService.class);
    private static final ConcurrentHashMap<Thread, DiService> threads = new ConcurrentHashMap<>();
    private static final TimerInstance timerService = new TimerService().schedule(DiService::threadsClean, 1000, 2000);
    protected final HashMap<Class<?>, Object> singletons = new HashMap<>();
    protected final List<DiService> children = new ArrayList<>();
    private final HashMap<Type, List<Type>> mappings = new HashMap<>();
    private final HashMap<Type, List<Type>> transientMappings = new HashMap<>();
    private final HashMap<String, Object> namedMappings = new HashMap<>();
    protected TpmScopeType scope = TpmScopeType.GLOBAL;
    protected DiService parent = null;

    public DiService() {
        threads.clear();
        singletons.put(DiService.class, this);
    }

    protected DiService(TpmScopeType scope, DiService parent) {
        this.scope = scope;
        this.parent = parent;
        this.parent.children.add(this);
        singletons.put(DiService.class, this);
    }

    public static void threadsClean() {
        var threadsToClean = new ArrayList<Thread>();
        for (var thread : threads.keySet()) {
            if (!thread.isAlive()) {
                threadsToClean.add(thread);
            }
        }
        for (var thread : threadsToClean) {
            var context = threads.get(thread);
            context.destroy();
            threads.remove(thread);
        }
    }

    public static DiService getThreadContext() {
        return threads.get(Thread.currentThread());
    }

    public static void setThreadContext(DiService threadContext) {
        if(threadContext==null) {
            threads.remove(Thread.currentThread());
        }else {
            threads.put(Thread.currentThread(), threadContext);
        }
    }

    public static List<String> getTags(Object instance) {
        var result = new ArrayList<String>();
        var tpmNamed = instance.getClass().getAnnotation(TpmNamed.class);
        if (tpmNamed != null) {
            result.addAll(Arrays.asList(tpmNamed.tags()));
        }
        var tpmService = instance.getClass().getAnnotation(TpmService.class);
        if (tpmService != null) {
            result.addAll(Arrays.asList(tpmService.tags()));
        }
        var tpmTransient = instance.getClass().getAnnotation(TpmTransient.class);
        if (tpmTransient != null) {
            result.addAll(Arrays.asList(tpmTransient.tags()));
        }
        return result;
    }

    private void destroy() {
        for (var item : children) {
            try {
                item.destroy();
            }catch (Exception e) {}
        }
        children.clear();
        for (var item : singletons.values()) {
            try {
                destroy(item);
            }catch (Exception e) {}
        }
        singletons.clear();
        if (parent != null) {
            parent.children.remove(this);
        }
        namedMappings.clear();
        parent = null;
        transientMappings.clear();
        mappings.clear();
        scope = TpmScopeType.NONE;
    }

    public DiService createChildScope(TpmScopeType scope) {
        if (scope == TpmScopeType.CUSTOM) {
            return new DiService(scope, this);
        } else if (scope == TpmScopeType.GLOBAL) {
            return this;
        } else {
            var svc = new DiService(scope, this);
            setThreadContext(svc);
            return svc;
        }
    }

    public void register(Class<?> clazz, Object instance) {
        singletons.put(clazz, instance);
        bind(instance.getClass(), mappings);
    }

    public void register(Object instance) {
        singletons.put(instance.getClass(), instance);
        bind(instance.getClass(), mappings);
    }

    public void overwrite(Class<?> clazz, Object instance) {
        singletons.put(clazz, instance);
        var implOfInt = new ArrayList<Type>();
        implOfInt.add(instance.getClass());
        mappings.put(clazz, implOfInt);
    }

    public void registerNamed(String name, Object instance) {
        namedMappings.put(name, instance);
    }

    public void loadPackage(String packageName) {
        Reflections packageReflections = new Reflections(packageName);
        var serviceAnnotations = new HashSet<>(List.of(TpmService.class));
        serviceAnnotations.stream()
                .map(packageReflections::getTypesAnnotatedWith)
                .flatMap(Set::stream)
                .forEach((t) -> this.bind(t, mappings));
        var transientAnnotations = new HashSet<>(List.of(TpmTransient.class));
        transientAnnotations.stream()
                .map(packageReflections::getTypesAnnotatedWith)
                .flatMap(Set::stream)
                .forEach((t) -> this.bind(t, transientMappings));

    }

    public void bind(Class<?> t) {
        this.bind(t, mappings);
    }

    public void bindTransient(Class<?> t) {
        this.bind(t, transientMappings);
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
            if (!mappings.get(i).contains(t)) {
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

            for (Class<?> anInterface : interfaces) {
                if (!interfacesFound.contains(anInterface)) {
                    interfacesFound.add(anInterface);
                    getAllInterfaces(anInterface, interfacesFound);
                }
            }
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType parameterizedType) {
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

    public <T> T getInstance(Class<T> clazz, String... tags) {
        return (T) getInstanceInternal(this, null, clazz, tags);
    }

    public <T> T getNamedInstance(String name, Class<T> clazz, String... tags) {
        return (T) getInstanceInternal(this, name, clazz, tags);
    }

    private Object getInstanceInternal(DiService context, String name, Type type, String... tags) {
        var result = getInstancesInternal(name, Object.class, context, type, tags);
        if (result == null || result.isEmpty()) return null;
        return result.get(0);
    }

    public <T> List<T> getInstances(Class<T> clazz, String... tags) {
        return getInstancesInternal(null, clazz, this, clazz, tags);
    }


    private Object createInstance(DiService context, Class<?> clazz, boolean transi) {
        try {
            if (!transi) {
                var sl = this;
                while (sl != null) {
                    if (sl.singletons.containsKey(clazz)) {
                        return sl.singletons.get(clazz);
                    }
                    sl = sl.parent;
                }
            }
            var constructors = clazz.getConstructors();
            var clazzNamed = clazz.getAnnotation(TpmNamed.class);
            Constructor constructor = null;
            if (constructors.length == 1) {
                constructor = constructors[0];
            } else {
                for (var constr : constructors) {
                    if (null != constr.getAnnotation(TpmConstructor.class)) {
                        if (constructor != null) {
                            throw new RuntimeException("Duplicate TpmConstructor constructor found");
                        }
                        constructor = constr;
                    }
                }
            }

            if (constructor == null) {
                throw new RuntimeException("No TpmConstructor found");
            }
            Object result = null;
            if (constructor.getParameterCount() == 0) {
                var singleton = getSingletonMapping(clazz);
                if (transi) {
                    result = postConstruct(constructor.newInstance());
                } else if (singleton.isPresent()) {
                    result = singleton.get();
                } else {
                    singletons.put(clazz, postConstruct(constructor.newInstance()));
                    result = singletons.get(clazz);
                }
            } else {
                var parameters = constructor.getParameters();
                var values = new Object[parameters.length];
                for (var i = 0; i < parameters.length; i++) {
                    var parameter = parameters[i];
                    var parameterAnnotation = parameter.getAnnotation(TpmNamed.class);
                    var tags = new String[]{};
                    String named = null;
                    if (parameterAnnotation != null) {
                        tags = parameterAnnotation.tags();
                        named = parameterAnnotation.value();
                    }
                    if (List.class.isAssignableFrom(parameter.getType())) {
                        var args = ((ParameterizedType) parameter.getParameterizedType()).getActualTypeArguments();
                        if (args.length == 1) {
                            var arg = args[0];
                            values[i] = getInstancesInternal(named, clazz, context, arg, tags);
                        }
                    } else {
                        var parametrizedType = parameter.getParameterizedType();
                        if (parametrizedType instanceof ParameterizedType) {
                            values[i] = getInstanceInternal(context, named, parametrizedType, tags);
                        } else {
                            values[i] = getInstanceInternal(context, named, parameter.getType(), tags);
                        }
                    }
                }
                var singleton = getSingletonMapping(clazz);
                if (transi) {
                    result = postConstruct(constructor.newInstance(values));
                } else if (singleton.isPresent()) {
                    result = singleton.get();
                } else {
                    singletons.put(clazz, postConstruct(constructor.newInstance(values)));
                    result = singletons.get(clazz);
                }
            }
            if (clazzNamed != null) {
                namedMappings.put(clazzNamed.value(), result);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate " + clazz, e);
        }
    }

    private Object destroy(Object o) {
        var ms = o.getClass().getMethods();
        for (var m : ms) {
            if (m.getAnnotation(TpmDestroy.class) != null) {
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

    private Object postConstruct(Object o) {
        var ms = o.getClass().getMethods();
        for (var m : ms) {
            if (m.getAnnotation(TpmPostConstruct.class) != null) {
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

    private <T> Optional<T> getNamedMapping(Class<T> clazz, String name) {
        if (name != null) {
            var parentItem = this;
            while (parentItem != null) {
                if (parentItem.namedMappings.containsKey(name)) {
                    Object instance = namedMappings.get(name);
                    if (clazz.isAssignableFrom(instance.getClass())) {
                        return Optional.of((T) instance);
                    }
                    return Optional.of(null);
                }
                parentItem = parentItem.parent;
            }

        }
        return Optional.empty();
    }

    private <T> Optional<T> getSingletonMapping(Class<T> clazz) {
        var parentItem = this;
        while (parentItem != null) {
            if (parentItem.singletons.containsKey(clazz)) {
                Object instance = parentItem.singletons.get(clazz);
                return Optional.of((T) instance);
            }
            parentItem = parentItem.parent;
        }
        return Optional.empty();
    }


    private <T> List<T> getInstancesInternal(String name, Class<T> clazz, DiService context, Type type, String... tags) {
        if (scope == TpmScopeType.NONE) {
            throw new RuntimeException("Invalid Context");
        }
        var result = new ArrayList<T>();
        if (name != null) {
            var named = getNamedMapping(clazz, name);
            if (named.isPresent()) {
                result.add(named.get());
                return result;
            }
        }
        var parentItem = this;
        while (parentItem != null) {
            var data = parentItem.mappings.get(type);
            var transi = false;
            if (data == null) {
                data = parentItem.transientMappings.get(type);
                if (data != null) {
                    transi = true;
                } else {
                    parentItem = parentItem.parent;
                    continue;
                }
            }

            for (var i : data) {
                var annotation = ((Class<?>) i).getAnnotation(TpmService.class);
                if (annotation != null) {
                    if (name != null && !name.equalsIgnoreCase(annotation.value())) {
                        continue;
                    }
                }
                if (tags.length > 0) {
                    if (annotation != null) {
                        var iTags = annotation.tags();
                        if (iTags.length != tags.length) {
                            continue;
                        }
                        if (!Arrays.asList(iTags).containsAll(Arrays.asList(tags))) {
                            continue;
                        }
                        result.add((T) createInstance(context, (Class<?>) i, transi));
                    }
                } else {
                    result.add((T) createInstance(context, (Class<?>) i, transi));
                }

            }
            parentItem = parentItem.parent;
        }
        return result;
    }

    public List<Class<?>> getDefinitions(Class<?> type, String... tags) {
        return getNamedDefinitions(type, null, tags);
    }

    public List<Class<?>> getNamedDefinitions(Class<?> type, String name, String... tags) {
        if (scope == TpmScopeType.NONE) {
            throw new RuntimeException("Invalid Context");
        }
        var result = new ArrayList<Class<?>>();

        var parentItem = this;
        while (parentItem != null) {
            var data = parentItem.mappings.get(type);
            var transi = false;
            if (data == null) {
                data = parentItem.transientMappings.get(type);
                if (data != null) {
                    transi = true;
                } else {
                    parentItem = parentItem.parent;
                    continue;
                }
            }

            for (var i : data) {
                var annotation = ((Class<?>) i).getAnnotation(TpmService.class);
                if (annotation != null) {
                    if (name != null && !name.equalsIgnoreCase(annotation.value())) {
                        continue;
                    }
                }
                if (tags.length > 0) {
                    if (annotation != null) {
                        var iTags = annotation.tags();
                        if (iTags.length != tags.length) {
                            continue;
                        }
                        if (!Arrays.asList(iTags).containsAll(Arrays.asList(tags))) {
                            continue;
                        }
                        result.add((Class<?>) i);
                    }
                } else {
                    result.add((Class<?>) i);
                }

            }
            parentItem = parentItem.parent;
        }
        return result;
    }

    public void clean() {
        try {
            this.destroy();
        }catch (Exception e) {}
        for (int i = 0; i < children.size(); i++) {
            var item = children.get(i);
            item.clean();
        }
        children.clear();
        Object[] array = singletons.values().toArray();
        for (int i = 0; i < array.length; i++) {
            var item = array[i];
            destroy(item);
        }
        singletons.clear();
        if (parent != null) {
            parent.children.remove(this);
        }
        namedMappings.clear();
        parent = null;
        threads.clear();

    }
}
