package org.kendar.apis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.Primitives;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.SwaggerEnricher;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.Header;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.filters.FiltersConfiguration;
import org.kendar.apis.matchers.ApiMatcher;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.di.annotations.TpmService;
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("HttpUrlsUsage")
@TpmService
@HttpTypeFilter(
        blocking = true)
public class SwaggerApi implements FilteringClass {
    private static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(SwaggerApi.class);
    private final List<SwaggerEnricher> enrichers;
    private final FiltersConfiguration filtersConfiguration;
    private final int port;

    public SwaggerApi(FiltersConfiguration filtersConfiguration,
                      List<SwaggerEnricher> enrichers, GlobalSettings settings) {

        this.filtersConfiguration = filtersConfiguration;
        this.port = settings.getApiPort();
        this.enrichers = enrichers;
    }

    @HttpMethodFilter(
            pathAddress = "/api/swagger/map.json",
            method = "GET", id = "GET /api/swagger/map.json")
    @TpmDoc(
            description = "Retrieve the swagger api",
            responses = @TpmResponse(
                    body = String.class,
                    description = "The json for the swagger api"
            ),
            tags = {"base/swagger"})
    public void loadSwagger(Request reqp, Response resp) throws JsonProcessingException {

        OpenAPI swagger = new OpenAPI();
        if (reqp.getPort() > 0) {
            swagger.addServersItem(new Server().url("http://" + reqp.getHost() + ":" + reqp.getPort()));
        } else {
            swagger.addServersItem(new Server().url("http://" + reqp.getHost() + ":" + port));
        }
        swagger.setInfo(new Info()
                .title("Local API")
                .version("1.0.0"));
        Map<String, Schema> schemas = new HashMap<>();
        Map<String, PathItem> expectedPaths = new HashMap<>();
        for (var kvp : filtersConfiguration.filters) {
            handleSingleFilter(swagger, schemas, expectedPaths, kvp);

        }

        for (var enricher : enrichers) {
            enricher.enrich(swagger);
        }
        publishResponse(resp, swagger, schemas);
    }

    private void handleSingleFilter(OpenAPI swagger, Map<String, Schema> schemas, Map<String, PathItem> expectedPaths, FilterDescriptor filter) {
        TpmDoc doc = filter.getTpmDoc();
        if (doc == null) return;
        var mf = filter.getMethodFilter();
        if (!expectedPaths.containsKey(filter.getMethodFilter().pathAddress())) {
            expectedPaths.put(filter.getMethodFilter().pathAddress(), new PathItem());
        } else {
            log.trace("Duplicate path {} {}", mf.method(), filter.getMethodFilter().pathAddress());
        }
        var expectedPath = expectedPaths.get(filter.getMethodFilter().pathAddress());

        if (doc.todo()) {
            setupTodoApi(swagger, filter, doc, expectedPath);
        } else {
            setupRealApi(swagger, schemas, filter, doc, expectedPath);
        }
    }

    private void publishResponse(Response resp, OpenAPI swagger, Map<String, Schema> schemas) {
        try {
            var components = swagger.getComponents();
            if (components == null) {
                components = new Components();
            }
            var scc = components;
            schemas.forEach(scc::addSchemas);
            swagger.components(components);
            String swaggerJson = Json.mapper().writeValueAsString(swagger);
            OpenAPI rebuilt = Json.mapper().readValue(swaggerJson, OpenAPI.class);
            resp.setResponseText(mapper.toJsonNode(Json.mapper().writeValueAsString(rebuilt)));
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);

        } catch (Exception ignored) {

        }
    }

    private void setupTodoApi(OpenAPI swagger, FilterDescriptor descriptor, TpmDoc doc, PathItem expectedPath) {
        var matcher = descriptor.getMatchers().stream().filter(m -> m instanceof ApiMatcher).findFirst();
        if (matcher.isEmpty()) return;
        var filter = (ApiMatcher) matcher.get();
        var meth = filter.getMethod();
        var operation = new Operation();
        operation.description("TODO");
        var parameters = new ArrayList<Parameter>();
        if (doc.path() != null) {
            for (var res : doc.path()) {
                parameters.add(new PathParameter()
                        .name(res.key())
                        .schema(new Schema()
                                .type(res.type())
                                .example(res.example())));
            }
        }

        operation.parameters(parameters);
        if (doc.tags() != null && doc.tags().length > 0) {
            operation.tags(Arrays.asList(doc.tags()));
        }
        setupMethod(expectedPath, operation, meth);
        swagger.path(descriptor.getMethodFilter().pathAddress(), expectedPath);


        swagger
                .path("/health/{pp}", expectedPath);
    }

    private void setupRealApi(OpenAPI swagger, Map<String, Schema> schemas, FilterDescriptor filter, TpmDoc doc, PathItem expectedPath) {
        List<Parameter> parameters = new ArrayList<>();

        prepareQuery(doc, parameters);
        prpearePath(doc, parameters);
        prepareHeaders(doc, parameters);

        var apiResponses = new ApiResponses();
        // Setup the models for the response
        if (doc.responses() == null || doc.responses().length == 0) {
            buildEmptyResponse(apiResponses);
        } else {
            var responses = new HashMap<Integer, List<mt>>();
            for (var res : doc.responses()) {
                prepareResponse(schemas, responses, res, filter);
            }
            for (var singres : responses.entrySet()) {
                buildResponse(apiResponses, singres);
            }
        }

        // Setup the models for the requests
        if (doc.requests() != null && doc.requests().length > 0) {
            for (var res : doc.requests()) {
                buildRequest(swagger, schemas, filter, doc, expectedPath, parameters, apiResponses, res);
            }
        } else {
            buildEmptyRequest(swagger, schemas, filter, doc, expectedPath, parameters, apiResponses);
        }
    }

    private void buildResponse(ApiResponses apiResponses, Map.Entry<Integer, List<mt>> singres) {
        var toAddResponse = new ApiResponse();
        var content = new Content();
        for (var singresitem : singres.getValue()) {
            content.addMediaType(singresitem.content, singresitem.mediaType);

            for (var hea : singresitem.headers.values()) {
                toAddResponse.addHeaderObject(
                        hea.key(),
                        new io.swagger.v3.oas.models.headers.Header()
                                .schema(getSchemaTpm(String.class))
                                .description(hea.description())
                                .example(hea.value())
                );
            }
        }
        toAddResponse.setContent(content);
        toAddResponse.setDescription(singres.getKey() + "");
        apiResponses.addApiResponse(singres.getKey() + "", toAddResponse);
    }

    private void prepareResponse(Map<String, Schema> schemas, HashMap<Integer, List<mt>> responses, TpmResponse res,
                                 FilterDescriptor descriptor) {
        var hasBody = extractSchemasForMethod(schemas, res.body(), res.bodyMethod(), descriptor);

        if (hasBody) {
            var bodyClass = res.body();
            if (bodyClass == Object.class && res.bodyMethod() != null && !res.bodyMethod().isEmpty()) {
                bodyClass = (Class<?>) descriptor.invokeOnFilterClass(res.bodyMethod());
            }
            var mmt = new mt();
            if (res.headers() != null) {
                for (var hea : res.headers()) {
                    mmt.headers.put(hea.key(), hea);
                }
            }
            var schema = getSchemaTpm(bodyClass);
            var mediaType = new MediaType().schema(schema);
            if (res.examples() != null) {
                for (var ex : res.examples()) {
                    mediaType.addExamples(ex.description(), new Example().value(ex.example()));
                }
            }
            if (!responses.containsKey(res.code())) {
                responses.put(res.code(), new ArrayList<>());
            }
            mmt.description = res.description();
            mmt.content = res.content();
            mmt.mediaType = mediaType;
            responses.get(res.code()).add(mmt);
        }
    }

    private void buildRequest(OpenAPI swagger, Map<String, Schema> schemas, FilterDescriptor filter, TpmDoc doc, PathItem expectedPath, List<Parameter> parameters, ApiResponses apiResponses, TpmRequest req) {
        var resBody = req.body();
        if (resBody == Object.class && req.bodyMethod() != null && !req.bodyMethod().isEmpty()) {
            resBody = (Class<?>) filter.invokeOnFilterClass(req.bodyMethod());
        }
        var resExamples = req.examples();
        var resAccept = req.accept();
        var resOptional = req.optional();

        setupRequest(swagger, schemas, filter, doc, expectedPath, parameters, apiResponses, resBody, resExamples, resAccept, resOptional);
    }

    private void buildEmptyRequest(OpenAPI swagger, Map<String, Schema> schemas, FilterDescriptor filter, TpmDoc doc,
                                   PathItem expectedPath, List<Parameter> parameters, ApiResponses apiResponses) {
        Class<?> resBody = Object.class;
        org.kendar.annotations.multi.Example[] resExamples = null;
        String resAccept = null;

        setupRequest(swagger, schemas, filter, doc, expectedPath, parameters, apiResponses, resBody, resExamples, resAccept, true);
    }

    private void buildEmptyResponse(ApiResponses apiResponses) {
        var toAddResponse = new ApiResponse();
        toAddResponse.setDescription("200");
        apiResponses.addApiResponse("200", toAddResponse);
    }

    private void prepareQuery(TpmDoc doc, List<Parameter> parameters) {
        // Setup query strings
        if (doc.query() != null) {
            for (var res : doc.query()) {
                parameters.add(new QueryParameter()
                        .name(res.key())
                        .schema(new Schema()
                                .type(res.type())
                                .example(res.example())));
            }
        }
    }

    private void prpearePath(TpmDoc doc, List<Parameter> parameters) {
        // Setup path variables
        if (doc.path() != null) {
            for (var res : doc.path()) {
                var examples = new ArrayList<String>();
                if (res.example() != null) {
                    for (var ex : res.example()) {
                        if (ex.isEmpty()) continue;
                        examples.add(ex);
                    }
                }
                var enums = new ArrayList<String>();
                if (res.allowedValues() != null) {
                    for (var ex : res.allowedValues()) {
                        if (ex.isEmpty()) continue;
                        enums.add(ex);
                    }
                }
                var schema = new Schema()
                        .type(res.type())
                        .examples(examples);
                if (!enums.isEmpty()) {
                    schema._enum(enums);
                }
                parameters.add(new PathParameter()
                        .name(res.key())
                        .schema(schema));
            }
        }
    }

    private void prepareHeaders(TpmDoc doc, List<Parameter> parameters) {
        // Setup header variables
        if (doc.header() != null) {
            for (var res : doc.header()) {
                parameters.add(new HeaderParameter()
                        .name(res.key())
                        .description(res.key())
                        .schema(new Schema()
                                .type("string")
                                .example(res.value())));
            }
        }
    }

    private void setupRequest(OpenAPI swagger, Map<String, Schema> schemas, FilterDescriptor descriptor, TpmDoc doc,
                              PathItem expectedPath, List<Parameter> parameters, ApiResponses apiResponses, Class<?> resBody, org.kendar.annotations.multi.Example[] resExamples,
                              String resAccept, boolean optionalBody) {
        var matcher = descriptor.getMatchers().stream().filter(m -> m instanceof ApiMatcher).findFirst();
        if (matcher.isEmpty()) return;
        var filter = (ApiMatcher) matcher.get();
        var hasBody = extractSchemasForMethod(schemas, resBody, null, descriptor);

        var operation = new Operation();
        if (hasBody) {
            setupRequestBody(resBody, resExamples, resAccept, operation, optionalBody);
        }
        operation.description(doc.description());
        operation.responses(apiResponses);
        operation.parameters(parameters);
        var meth = filter.getMethod();
        if (doc.tags() != null && doc.tags().length > 0) {
            operation.tags(Arrays.asList(doc.tags()));
        }
        if (doc.security() != null) {
            for (var sec : doc.security()) {
                SecurityRequirement securityRequirement = new SecurityRequirement();
                securityRequirement.put(sec.name(), Arrays.stream(sec.scopes()).collect(Collectors.toList()));
                operation.addSecurityItem(securityRequirement);
            }
        }
        setupMethod(expectedPath, operation, meth);

        swagger.path(descriptor.getMethodFilter().pathAddress(), expectedPath);
    }

    private void setupRequestBody(Class<?> resBody, org.kendar.annotations.multi.Example[] resExamples,
                                  String resAccept, Operation operation, boolean optionalBody) {
        var schema = getSchemaTpm(resBody);
        var mediaType = new MediaType().schema(schema);
        if (resExamples != null) {
            for (var ex : resExamples) {
                mediaType.addExamples(ex.description(), new Example().value(ex.example()));
            }
        }
        var content = new Content()
                .addMediaType(resAccept,
                        mediaType);

        RequestBody requestBody = new RequestBody().content(content).required(!optionalBody);
        operation.requestBody(requestBody);
    }

    private void setupMethod(PathItem expectedPath, Operation operation, String meth) {
        if (meth.equalsIgnoreCase("GET")) {
            expectedPath.get(operation);
        } else if (meth.equalsIgnoreCase("POST")) {
            expectedPath.post(operation);
        } else if (meth.equalsIgnoreCase("PUT")) {
            expectedPath.put(operation);
        } else if (meth.equalsIgnoreCase("OPTIONS")) {
            expectedPath.options(operation);
        } else if (meth.equalsIgnoreCase("DELETE")) {
            expectedPath.delete(operation);
        }
    }

    private boolean extractSchemasForMethod(Map<String, Schema> schemas, Class<?> bodyRequest, String methodOnDescriptor, FilterDescriptor descriptor) {
        if (bodyRequest == Object.class) return false;
        if (bodyRequest == null && methodOnDescriptor != null && !methodOnDescriptor.isEmpty()) {
            bodyRequest = (Class<?>) descriptor.invokeOnFilterClass(methodOnDescriptor);
        }
        if (Primitives.isWrapperType(bodyRequest)) return true;
        if (bodyRequest.isPrimitive()) return true;
        if (bodyRequest.isArray()) {
            return extractSchemasForMethod(schemas, bodyRequest.getComponentType(), methodOnDescriptor, descriptor);
        }
        if (Collection.class.isAssignableFrom(bodyRequest)) return true;
        var request = ModelConverters.getInstance().readAll(bodyRequest);
        schemas.putAll(request);
        return true;
    }

    private Schema getSchemaTpm(Class<?> bodyRequest) {
        if (bodyRequest == byte[].class) {
            return new Schema().type("string").format("byte");

        }
        if (Primitives.isWrapperType(bodyRequest)) {
            return new Schema().type(Primitives.unwrap(bodyRequest).getSimpleName().toLowerCase(Locale.ROOT));
        }
        if (bodyRequest.isPrimitive()) {
            return new Schema().type(bodyRequest.getSimpleName().toLowerCase(Locale.ROOT));
        }
        if (bodyRequest == String.class) {
            return new Schema().type(bodyRequest.getSimpleName().toLowerCase(Locale.ROOT));
        }
        if (bodyRequest.isArray()) {
            return new Schema()
                    .type("array")
                    .items(getSchemaTpm(bodyRequest.getComponentType()));
        }
        if (List.class.isAssignableFrom(bodyRequest)) {
            return new Schema()
                    .type("array")
                    .items(getSchemaTpm(bodyRequest.getComponentType()));
        }
        if (Collection.class.isAssignableFrom(bodyRequest)) {
            return new Schema()
                    .type("array")
                    .items(getSchemaTpm(bodyRequest.getComponentType()));
        }

        return new Schema().$ref(bodyRequest.getSimpleName());
    }

    @Override
    public String getId() {
        return this.getClass().getName();
    }

    static class mt {
        public final Map<String, Header> headers = new HashMap<>();
        public String content;
        public MediaType mediaType;
        public String description;
    }
}
