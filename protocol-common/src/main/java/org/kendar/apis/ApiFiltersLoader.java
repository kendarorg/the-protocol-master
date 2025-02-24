package org.kendar.apis;

import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.converters.RequestResponseBuilderImpl;
import org.kendar.apis.filters.FiltersConfiguration;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.apis.utils.CustomFiltersLoader;
import org.kendar.apis.utils.MimeChecker;
import org.kendar.di.annotations.TpmPostConstruct;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.kendar.apis.ApiUtils.respondKo;
import static org.kendar.apis.ApiUtils.respondText;

@TpmService
public class ApiFiltersLoader implements CustomFiltersLoader, HttpHandler {
    private final List<FilteringClass> filteringClassList;
    private final FiltersConfiguration filtersConfiguration;
    private final List<GlobalPluginDescriptor> globalPluginDescriptors;
    private final RequestResponseBuilderImpl requestResponseBuilder = new RequestResponseBuilderImpl();
    private final Logger log = LoggerFactory.getLogger(ApiFiltersLoader.class);

    public ApiFiltersLoader(List<FilteringClass> filteringClassList,
                            FiltersConfiguration filtersConfiguration,
                            List<GlobalPluginDescriptor> globalPluginDescriptors) {
        this.filteringClassList = filteringClassList;
        this.filtersConfiguration = filtersConfiguration;
        this.globalPluginDescriptors = globalPluginDescriptors;
    }

    public static Method[] getAllMethodsInHierarchy(Class<?> objectClass) {
        Set<Method> allMethods = new HashSet<>();
        Method[] declaredMethods = objectClass.getDeclaredMethods();
        Method[] methods = objectClass.getMethods();
        if (objectClass.getSuperclass() != null) {
            Class<?> superClass = objectClass.getSuperclass();
            Method[] superClassMethods = getAllMethodsInHierarchy(superClass);
            allMethods.addAll(Arrays.asList(superClassMethods));
        }
        allMethods.addAll(Arrays.asList(declaredMethods));
        allMethods.addAll(Arrays.asList(methods));
        return allMethods.toArray(new Method[0]);
    }

    @TpmPostConstruct
    public void postConstruct() {
        for (var gp : globalPluginDescriptors) {
            getFilters().add(gp.getApiHandler());
        }
    }

    public List<FilteringClass> getFilters() {
        return filteringClassList;
    }

    private List<FilterDescriptor> getAnnotatedMethods(FilteringClass cl) {
        var result = new ArrayList<FilterDescriptor>();
        var typeFilter = cl.getClass().getAnnotation(HttpTypeFilter.class);
        for (Method m : getAllMethodsInHierarchy(cl.getClass())) {
            var methodFilter = m.getAnnotation(HttpMethodFilter.class);
            if (methodFilter == null) continue;
            var tpmDoc = m.getAnnotation(TpmDoc.class);
            result.add(new FilterDescriptor(this, typeFilter, methodFilter, m, cl, tpmDoc));
        }
        return result;
    }

    @Override
    public List<FilterDescriptor> loadFilters() {
        var result = new ArrayList<FilterDescriptor>();
        for (var filterClass : filteringClassList) {
            if (filterClass == null) continue;
            result.addAll(getAnnotatedMethods(filterClass));
        }
        var duplicateIds = new HashSet<>();
        for (var ds : result) {
            filtersConfiguration.filters.add(ds);
            if (ds.getId() == null || ds.getId().equalsIgnoreCase("null")) {
                ds.setId("null:" + UUID.randomUUID());
            }
            var id = ds.getId();
            if (duplicateIds.contains(id)) {
                throw new RuntimeException("Duplicate filter id " + id);
            }
            duplicateIds.add(id);
            filtersConfiguration.filtersById.put(id, ds);
            if (!filtersConfiguration.filtersByClass.containsKey(ds.getClassId())) {
                filtersConfiguration.filtersByClass.put(ds.getClassId(), new ArrayList<>());
            }
            filtersConfiguration.filtersByClass.get(ds.getClassId()).add(ds);
        }
        filtersConfiguration.filters.sort(Comparator.comparingInt(FilterDescriptor::getPriority).reversed());

        return result;
    }

    @Override
    public boolean handle(
            Request request,
            Response response)
            throws InvocationTargetException, IllegalAccessException {
        var config = filtersConfiguration;
        if (config == null) return false;
        var possibleMatches = new ArrayList<FilterDescriptor>();
        for (var filterEntry : config.filters) {
            if (!filterEntry.matches(request)) continue;
            possibleMatches.add(filterEntry);
        }
        for (var filterEntry : possibleMatches) {
            if (filterEntry.execute(request, response)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        Response response = new Response();
        try {
            var request = requestResponseBuilder.fromExchange(httpExchange, "http");
            if (!handle(request, response)) {
                response.setStatusCode(404);
                respondText(response, request.buildUrl() + " Not Found");
            }
        } catch (Exception e) {
            respondKo(response, e);
        }
        sendResponse(response, httpExchange);
    }

    private void sendResponse(Response response, HttpExchange httpExchange) throws IOException {
        byte[] data = new byte[0];
        var dataLength = 0;
        if (requestResponseBuilder.hasBody(response)) {
            if (MimeChecker.isBinary(response) && response.getResponseText() instanceof BinaryNode) {
                data = ((BinaryNode) response.getResponseText()).binaryValue();
            } else if (response.getResponseText() != null) {

                if (MimeChecker.isJson(response.getFirstHeader(ConstantsHeader.CONTENT_TYPE))) {
                    data = response.getResponseText().toString().getBytes(StandardCharsets.UTF_8);
                } else if (response.getResponseText() instanceof TextNode) {
                    data = response.getResponseText().textValue().getBytes(StandardCharsets.UTF_8);
                } else if (response.getResponseText() instanceof BinaryNode) {
                    data = ((BinaryNode) response.getResponseText()).binaryValue();
                }
            }
            if (data.length > 0) {
                dataLength = data.length;
            }
        }
        response.addHeader("access-control-allow-credentials", "false");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "*");
        response.addHeader("Access-Control-Allow-Headers", "*");
        response.addHeader("Access-Control-Max-Age", "86400");
        response.addHeader("Access-Control-Expose-Headers", "*");
        var ignoreContentLength = shouldIgnoreContentLength(response.getStatusCode());
        for (var header : response.getHeaders().entrySet()) {
            for (var h : header.getValue()) {
                if (header.getKey().equalsIgnoreCase(ConstantsHeader.CONTENT_LENGTH)) {
                    if (ignoreContentLength) {
                        continue;
                    }
                }
                httpExchange.getResponseHeaders().add(header.getKey(), h);
            }
        }
        try {
            if (ignoreContentLength) dataLength = -1;
            httpExchange.sendResponseHeaders(response.getStatusCode(), dataLength);
        } catch (IOException ex) {
            if (!ex.getMessage().equalsIgnoreCase("output stream is closed")) {
                throw new IOException(ex);
            }
        }

        try {
            if (dataLength > 0) {
                OutputStream os = httpExchange.getResponseBody();
                os.write(data);
                os.flush();
                os.close();
            } else {
                try {
                    OutputStream os = httpExchange.getResponseBody();

                    os.write(new byte[0]);
                    os.flush();
                    os.close();
                } catch (Exception ex) {
                    //log.trace(ex.getMessage());
                }
            }
        } catch (Exception ex) {
            //log.error(ex.getMessage(), ex);
        }
    }


    private boolean shouldIgnoreContentLength(int rCode) {
        return ((rCode >= 100 && rCode < 200) /* informational */
                || (rCode == 204)           /* no content */
                || (rCode == 304));
    }

    public FiltersConfiguration getConfig() {
        return filtersConfiguration;
    }
}
