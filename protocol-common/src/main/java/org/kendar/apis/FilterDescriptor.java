package org.kendar.apis;


import org.apache.commons.lang3.ClassUtils;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.TpmMatcher;
import org.kendar.annotations.concrete.HttpMethodFilterConcrete;
import org.kendar.annotations.concrete.HttpTypeFilterConcrete;
import org.kendar.annotations.concrete.TpmDocConcrete;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.matchers.ApiMatcher;
import org.kendar.apis.matchers.FilterMatcher;
import org.kendar.apis.utils.CustomFiltersLoader;
import org.kendar.apis.utils.GenericFilterExecutor;
import org.kendar.apis.utils.IdBuilder;
import org.kendar.exceptions.ApiException;
import org.kendar.plugins.base.ProtocolApiHandler;
import org.kendar.plugins.base.ProtocolPluginApiHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * It's the "instance wrapper" of a method of a controller
 */
public class FilterDescriptor {

    private static final Pattern namedGroupsPattern =
            Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z\\d]*)>");
    private static final Logger log = LoggerFactory.getLogger(FilterDescriptor.class);
    private final int priority;
    private final boolean methodBlocking;
    private final boolean typeBlocking;
    private final Object filterClass;
    private final List<String> pathSimpleMatchers = new ArrayList<>();
    private final CustomFiltersLoader loader;
    private final List<String> pathMatchers = new ArrayList<>();
    private List<FilterMatcher> matchers = new ArrayList<>();
    private TpmMatcher[] extraMatches;
    private String description;
    private HttpTypeFilter typeFilter;
    private HttpMethodFilter methodFilter;
    private TpmDocConcrete doc;
    private Method callback;
    private String id;

    public FilterDescriptor(
            CustomFiltersLoader loader,
            HttpTypeFilter typeFilter,
            HttpMethodFilter methodFilter,
            Method callback,
            FilteringClass filterClass,
            TpmDoc tpmDoc) {
        if (tpmDoc != null) {
            this.doc = new TpmDocConcrete(tpmDoc);
            var tags = this.doc.tags();
            for (int i = 0; i < tags.length; i++) {
                var tag = tags[i];
                tags[i] = replaceFilterSpecific(tag, filterClass);
            }
        }
        this.loader = loader;
        var id = methodFilter.id();
        if (id != null && !id.isEmpty()) {
            id = replaceFilterSpecific(id, filterClass);
        }
        this.id = IdBuilder.buildId(typeFilter, methodFilter, filterClass, id);
        this.description = methodFilter.description();
        this.callback = callback;
        this.filterClass = filterClass;
        try {
            this.id = IdBuilder.buildId(typeFilter, methodFilter, filterClass, id);
        } catch (IncompleteAnnotationException ex) {
            throw new ApiException("Missing id", ex);
        }
        var pathPattern = methodFilter.pathPattern();
        var pathAddress = methodFilter.pathAddress();
        pathPattern = replaceFilterSpecific(pathPattern, filterClass);
        pathAddress = replaceFilterSpecific(pathAddress, filterClass);

        var matcher = new ApiMatcher(typeFilter.hostAddress(), typeFilter.hostPattern(),
                methodFilter.method(), pathPattern, pathAddress);
        matcher.initialize();
        priority = typeFilter.priority();
        methodBlocking = methodFilter.blocking();
        typeBlocking = typeFilter.blocking();
        matchers.add(matcher);
        this.extraMatches = methodFilter.matcher();
        this.typeFilter = buildTypeFilter();
        this.methodFilter = buildMethodFilter();
    }

    public FilterDescriptor(
            CustomFiltersLoader loader,
            GenericFilterExecutor executor) {
        this.loader = loader;

        for (var method : executor.getClass().getMethods()) {
            if (method.getName().equalsIgnoreCase("run")) {
                this.callback = method;
                break;
            }
        }
        this.filterClass = executor;
        this.id = executor.getId();
        for (var matcher : executor.getMatchers()) {
            matcher.initialize();
            if (!matcher.validate()) {
                throw new ApiException("Invalid filter");
            }
        }
        matchers = executor.getMatchers();

        priority = executor.getPriority();
        methodBlocking = executor.isMethodBlocking();
        typeBlocking = executor.isTypeBlocking();
        this.typeFilter = buildTypeFilter();
        this.methodFilter = buildMethodFilter();
    }

    public static List<String> getAllMatches(String text, String regex) {
        List<String> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return matches;
        }
        var m = Pattern.compile("(?=(" + regex + "))").matcher(text);
        while (m.find()) {
            matches.add(m.group(1));
        }
        return matches;
    }

    private static List<String> getAllReflectionPatterns(String text) {
        return getAllMatches(text, "\\{#[a-zA-Z_\\-\\.]+\\}");
    }

    private static List<String> getNamedGroupCandidates(String regex) {
        Set<String> matchedGroups = new TreeSet<>();
        var m = namedGroupsPattern.matcher(regex);
        while (m.find()) {
            matchedGroups.add(m.group(1));
        }
        return new ArrayList<>(matchedGroups);
    }

    public List<FilterMatcher> getMatchers() {
        return matchers;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassId() {
        return filterClass.getClass().getName();
    }

    private HttpMethodFilter buildMethodFilter() {
        var matcherUnknown = matchers.get(0);
        if (ClassUtils.isAssignable(matcherUnknown.getClass(), ApiMatcher.class)) {
            var matcher = (ApiMatcher) matcherUnknown;
            return new HttpMethodFilterConcrete(methodBlocking,
                    matcher.getPathAddress(), matcher.getPathPatternReal(), matcher.getMethod(),
                    description,
                    id, extraMatches);
        } else {
            return new HttpMethodFilterConcrete(methodBlocking,
                    "*", null, "*",
                    description,
                    id, extraMatches);
        }
    }

    private HttpTypeFilter buildTypeFilter() {
        var matcherUnknown = matchers.get(0);
        if (ClassUtils.isAssignable(matcherUnknown.getClass(), ApiMatcher.class)) {
            var matcher = (ApiMatcher) matcherUnknown;
            return new HttpTypeFilterConcrete(matcher.getHostAddress(), typeBlocking, priority, matcher.getHostPatternReal());
        } else {
            return new HttpTypeFilterConcrete("*", typeBlocking, priority, null);
        }
    }

    public int getPriority() {
        return priority;
    }

    public boolean matches(Request req) {
        for (var match : matchers) {
            if (!match.matches(req)) return false;
        }
        return true;
    }

    public Object invokeOnFilterClass(String name, Object... args) {
        if (name == null || name.isEmpty()) return null;
        try {
            var method = filterClass.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(filterClass, args);
        } catch (Exception e) {
            log.error("Error retrieving data from {}", filterClass.getClass().getName(), e);
            return null;
        }
    }

    /**
     * Run the callback associate with the address
     *
     * @param request
     * @param response
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public boolean execute(
            Request request, Response response)
            throws InvocationTargetException, IllegalAccessException {
        Object result = null;
        if (callback.getParameterCount() == 2) {
            result = callback.invoke(filterClass, request, response);
        } else if (callback.getParameterCount() == 1) {
            result = callback.invoke(filterClass, request);
        } else if (callback.getParameterCount() == 0) {
            result = callback.invoke(filterClass);
        }
        if (callback.getReturnType() == boolean.class) {
            if (result == null) {
                result = false;
            }
            return (boolean) result;
        } else {
            return true;
        }
    }


    public HttpTypeFilter getTypeFilter() {
        return typeFilter;
    }

    public void setTypeFilter(HttpTypeFilter typeFilter) {
        this.typeFilter = typeFilter;
    }

    public HttpMethodFilter getMethodFilter() {
        return methodFilter;
    }

    public void setMethodFilter(HttpMethodFilter methodFilter) {
        this.methodFilter = methodFilter;
    }

    public CustomFiltersLoader getLoader() {
        return loader;
    }

    public TpmDoc getTpmDoc() {

        var loc = this;
        if (this.doc == null) return null;
        return new TpmDocConcrete(doc);
    }

    private String replaceFilterSpecific(String pathPatternOri, FilteringClass filterClass) {
        var pathPattern = pathPatternOri;
        if (ProtocolPluginApiHandler.class.isAssignableFrom(filterClass.getClass())) {
            var ppah = (ProtocolPluginApiHandler) filterClass;
            pathPattern = pathPattern.replaceAll(
                    Pattern.quote("{#protocolInstanceId}"),
                    Matcher.quoteReplacement(ppah.getProtocolInstanceId()));

            pathPattern = pathPattern.replaceAll(
                    Pattern.quote("{#protocol}"),
                    Matcher.quoteReplacement(ppah.getProtocol()));
            pathPattern = pathPattern.replaceAll(
                    Pattern.quote("{#plugin}"),
                    Matcher.quoteReplacement(ppah.getPluginId()));
        } else if (ProtocolApiHandler.class.isAssignableFrom(filterClass.getClass())) {
            var ppah = (ProtocolApiHandler) filterClass;
            pathPattern = pathPattern.replaceAll(
                    Pattern.quote("{#protocolInstanceId}"),
                    Matcher.quoteReplacement(ppah.getProtocolInstanceId()));

            pathPattern = pathPattern.replaceAll(
                    Pattern.quote("{#protocol}"),
                    Matcher.quoteReplacement(ppah.getProtocol()));
        }
        return pathPattern;
    }
}

