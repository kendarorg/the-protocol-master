package org.kendar.apis;

import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.kendar.annotations.HamDoc;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.converters.RequestResponseBuilderImpl;
import org.kendar.apis.filters.FiltersConfiguration;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.apis.utils.CustomFiltersLoader;
import org.kendar.apis.utils.MimeChecker;
import org.kendar.plugins.apis.Ko;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ApiFiltersLoader implements CustomFiltersLoader, HttpHandler {
    private final List<FilteringClass> filteringClassList;
    private FiltersConfiguration filtersConfiguration;

    public List<FilteringClass> getFilters() {
        return filteringClassList;
    }

    public ApiFiltersLoader(List<FilteringClass> filteringClassList) {
        this.filteringClassList = filteringClassList;
        filtersConfiguration = new FiltersConfiguration();
        this.filteringClassList.add(new SwaggerApi(filtersConfiguration,new ArrayList<>()));
        this.filteringClassList.add(new MainWebSite(new FileResourcesUtils()));
    }

    private List<FilterDescriptor> getAnnotatedMethods(FilteringClass cl) {
        var result = new ArrayList<FilterDescriptor>();
        var typeFilter = cl.getClass().getAnnotation(HttpTypeFilter.class);
        for (Method m : cl.getClass().getMethods()) {
            var methodFilter = m.getAnnotation(HttpMethodFilter.class);
            if (methodFilter == null) continue;
            var hamDoc = m.getAnnotation(HamDoc.class);
            result.add(new FilterDescriptor(this, typeFilter, methodFilter, m, cl, hamDoc));
        }
        return result;
    }

    @Override
    public List<FilterDescriptor> loadFilters() {
        var result = new ArrayList<FilterDescriptor>();
        for (var filterClass : filteringClassList) {
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
            if(filterEntry.execute(request, response)) {
                return true;
            }
        }
        return false;
    }

    private static JsonMapper mapper = new JsonMapper();

    private RequestResponseBuilderImpl requestResponseBuilder =new RequestResponseBuilderImpl();
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        Request request = null;
        Response response = new Response();
        try {

            request = requestResponseBuilder.fromExchange(httpExchange, "http");
            if(!handle(request, response)){
                response.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
                response.setStatusCode(404);
                response.setResponseText(mapper.toJsonNode(new Ko(request.buildUrl()+" Not Found")));
            }
        }catch (Exception e) {
            response.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
            response.setResponseText(mapper.toJsonNode(new Ko(e.getMessage())));
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
                    //logger.trace(ex.getMessage());
                }
            }
        } catch (Exception ex) {
            //logger.error(ex.getMessage(), ex);
        }
    }


    private boolean shouldIgnoreContentLength(int rCode) {
        return ((rCode >= 100 && rCode < 200) /* informational */
                || (rCode == 204)           /* no content */
                || (rCode == 304));
    }
}
