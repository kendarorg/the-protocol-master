package org.kendar.apis.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.net.httpserver.HttpExchange;
import io.airlift.compress.zstd.ZstdInputStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.brotli.dec.BrotliInputStream;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.utils.*;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class RequestResponseBuilderImpl implements RequestResponseBuilder {

    protected static final JsonMapper mapper = new JsonMapper();
    private static final String H_SOAP_ACTION = "SOAPAction";
    private static final String H_AUTHORIZATION = "Authorization";
    private static final String BASIC_AUTH_MARKER = "basic";
    private static final String BASIC_AUTH_SEPARATOR = ":";
    private static Logger log;

    public RequestResponseBuilderImpl() {

    }

    private static void setupRequestHost(HttpExchange exchange, Request result) {
        result.setHost(exchange.getRequestURI().getHost());

        if (result.getHost() == null) {
            var data = exchange.getRequestHeaders().getFirst("Host").split(":", 2);
            if (data.length >= 1) {
                result.setHost(data[0]);
            }
        }
    }

    private static void setupRequestPort(HttpExchange exchange, Request result) {

        result.setPort(exchange.getRequestURI().getPort());
        if (result.getPort() <= 0) {
            var data = exchange.getRequestHeaders().getFirst("Host").split(":", 2);
            if (data.length == 2) {
                result.setPort(Integer.parseInt(data[1]));
            }
        }
        if (result.getPort() <= 0) {
            var data = result.getHost().split(":", 2);
            if (data.length == 2) {
                result.setPort(Integer.parseInt(data[1]));
            }
        }
    }

    private static void setupOptionalBody(HttpExchange exchange, Request result)
            throws IOException, FileUploadException {
        var headerContentType = result.getFirstHeader(ConstantsHeader.CONTENT_TYPE);
        if (headerContentType == null) {
            headerContentType = ConstantsMime.DEFAULT_CONTENT_TYPE;
        }

        if (RequestUtils.isMethodWithBody(result)) {

            var data = IOUtils.toByteArray(exchange.getRequestBody());
            var contentEncoding = "";
            if (null != result.getHeader(ConstantsHeader.CONTENT_ENCODING)) {
                contentEncoding = result.getFirstHeader(ConstantsHeader.CONTENT_ENCODING).toLowerCase(Locale.ROOT);
            }
            if (contentEncoding == null) contentEncoding = "";

            var brotli = contentEncoding.equalsIgnoreCase("br");
            var gzip = contentEncoding.equalsIgnoreCase("gzip");
            if (gzip) {
                InputStream bodyStream = new GZIPInputStream(new ByteArrayInputStream(data));
                data = IOUtils.toByteArray(bodyStream);
            } else if (brotli) {
                InputStream bodyStream = new BrotliInputStream(new ByteArrayInputStream(data));
                data = IOUtils.toByteArray(bodyStream);
            }

            // Calculate body
            if (headerContentType != null && headerContentType.toLowerCase(Locale.ROOT).startsWith("multipart")) {
                Pattern rp = Pattern.compile("boundary", Pattern.CASE_INSENSITIVE);
                var boundary = SimpleStringUtils.splitByString("boundary=", headerContentType)[1];
                result.setMultipartData(
                        RequestUtils.buildMultipart(data, boundary, result.getFirstHeader(ConstantsHeader.CONTENT_TYPE)));
            } else if (headerContentType != null && headerContentType
                    .toLowerCase(Locale.ROOT)
                    .startsWith("application/x-www-form-urlencoded")) {
                var requestText = new String(data, StandardCharsets.UTF_8);
                result.setPostParameters(RequestUtils.queryToMap(requestText));
            } else if (headerContentType != null && headerContentType
                    .toLowerCase(Locale.ROOT)
                    .startsWith(ConstantsMime.JSON_SMILE)) {
                result.setRequestText(JsonSmile.smileToJSON(data));
            } else {
                if (MimeChecker.isBinary(result)) {
                    result.setRequestText(new BinaryNode(data));
                } else {
                    if (MimeChecker.isJson(headerContentType)) {
                        result.setRequestText(mapper.toJsonNode(new String(data, StandardCharsets.UTF_8)));
                    } else {

                        result.setRequestText(new TextNode(new String(data, StandardCharsets.UTF_8)));
                    }
                }
            }
        }
    }

    private static void setupAuthHeaders(Request result) {
        var headerAuthorization = result.getFirstHeader(H_AUTHORIZATION);
        if (headerAuthorization != null
                && headerAuthorization.toLowerCase(Locale.ROOT).startsWith(BASIC_AUTH_MARKER)) {
            String base64Credentials = headerAuthorization.substring(BASIC_AUTH_MARKER.length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            // credentials = username:password
            final String[] values = credentials.split(BASIC_AUTH_SEPARATOR, 2);
            result.setBasicUsername(values[0]);
            result.setBasicPassword(values[1]);
        }
    }

    private static boolean isJsonNodeFull(JsonNode data) {
        if (data == null) return false;
        if (data instanceof TextNode) {
            if (data.textValue() == null) return false;
            return !data.textValue().isEmpty();
        } else if (data instanceof BinaryNode) {
            if (((BinaryNode) data).binaryValue() == null) return false;
            return ((BinaryNode) data).binaryValue().length > 0;
        } else return data instanceof JsonNode;
    }

    @Override
    public Request fromExchange(HttpExchange exchange, String protocol)
            throws IOException, FileUploadException {
        var result = new Request();
        result.setRemoteHost(exchange.getRemoteAddress().getAddress().getHostAddress());
        result.setProtocol(protocol.toLowerCase(Locale.ROOT));

        result.setQuery(RequestUtils.queryToMap(exchange.getRequestURI().getRawQuery()));


        setupRequestHost(exchange, result);
        setupRequestPort(exchange, result);
        result.setPath(exchange.getRequestURI().getRawPath());
        result.setMethod(exchange.getRequestMethod().toUpperCase(Locale.ROOT));
        result.setHeaders(RequestUtils.headersToMap(exchange.getRequestHeaders()));
        var headerContentType = result.getFirstHeader(ConstantsHeader.CONTENT_TYPE);

        result.setSoapRequest(result.getHeader(H_SOAP_ACTION) != null);
        setupAuthHeaders(result);

        setupOptionalBody(exchange, result);
        // result.sanitizedPath = RequestUtils.sanitizePath(result);
        return result;
    }

    @Override
    public boolean isMultipart(Request request) {
        var headerContentType = request.getFirstHeader(ConstantsHeader.CONTENT_TYPE);
        if (headerContentType == null) return false;
        return headerContentType.toLowerCase(Locale.ROOT).startsWith("multipart");
    }

    @Override
    public boolean hasBody(Request request) {
        var data = request.getRequestText();
        return isJsonNodeFull(data);
    }

    @Override
    public boolean hasBody(Response response) {
        var data = response.getResponseText();
        return isJsonNodeFull(data);
    }

    @Override
    public void fromHttpResponse(HttpResponse httpResponse, Response response)
            throws IOException {
        HttpEntity responseEntity = httpResponse.getEntity();

        var brotli = false;
        var zstd = false;
        String contentEncoding = "";
        if (responseEntity != null) {
            InputStream in = responseEntity.getContent();

            if (null != responseEntity.getContentEncoding()) {
                contentEncoding = responseEntity.getContentEncoding().getValue().toLowerCase(Locale.ROOT);
            }
            if (contentEncoding == null) contentEncoding = "";

            zstd = contentEncoding.equalsIgnoreCase("zstd");
            brotli = contentEncoding.equalsIgnoreCase("br");
            if (responseEntity.getContentType() != null &&
                    MimeChecker.isBinary(responseEntity.getContentType().getValue(), contentEncoding)) {

                if (zstd) {
                    response.setResponseText(new BinaryNode(IOUtils.toByteArray(new ZstdInputStream(in))));
                    response.removeHeader(ConstantsHeader.CONTENT_ENCODING);
                } else if (brotli) {
                    response.setResponseText(new BinaryNode(IOUtils.toByteArray(new BrotliInputStream(in))));
                    response.removeHeader(ConstantsHeader.CONTENT_ENCODING);
                } else {
                    response.setResponseText(new BinaryNode(IOUtils.toByteArray(in)));
                }
            } else {
                JsonNode responseText = null;
                var bytes = new byte[]{};
                if (zstd) {
                    bytes = IOUtils.toByteArray(new ZstdInputStream(in));
                    response.removeHeader(ConstantsHeader.CONTENT_ENCODING);
                } else if (brotli) {
                    bytes = IOUtils.toByteArray(new BrotliInputStream(in));
                    response.removeHeader(ConstantsHeader.CONTENT_ENCODING);
                } else {
                    bytes = IOUtils.toByteArray(in);
                }
                if (responseEntity.getContentType() != null &&
                        MimeChecker.isJsonSmile(responseEntity.getContentType()
                                .getValue())) {
                    responseText = JsonSmile.smileToJSON(bytes);
                } else if (responseEntity.getContentType() != null &&
                        MimeChecker.isJson(responseEntity.getContentType()
                                .getValue())) {
                    responseText = mapper.toJsonNode(new String(bytes, StandardCharsets.UTF_8));
                } else {
                    responseText = new TextNode(new String(bytes, StandardCharsets.UTF_8));
                }
                response.setResponseText(responseText);
            }
        } else {
            response.setResponseText(new TextNode(""));
        }
        response.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        for (var header : httpResponse.getAllHeaders()) {
            if (header.getName().equalsIgnoreCase(ConstantsHeader.TRANSFER_ENCODING)) continue;
            response.addHeader(header.getName(), header.getValue());
        }
        if (brotli || zstd) {
            response.removeHeader(ConstantsHeader.CONTENT_ENCODING);
        }
    }
}
