package org.kendar.http.data;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpRequest;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class RequestResponseBuilderImpl  {

    private static final String H_SOAP_ACTION = "SOAPAction";
    private static final String H_AUTHORIZATION = "Authorization";
    private static final String BASIC_AUTH_MARKER = "basic";
    private static final String BASIC_AUTH_SEPARATOR = ":";
    private static Logger logger;



    private static void setupRequestHost(MitmJavaProxyHttpRequest exchange, Request result) {
        result.setHost(exchange.getMethod().getURI().getHost());

        if (result.getHost() == null) {
            var data = Arrays.stream(exchange.getMethod().getAllHeaders()).filter(h->h.getName().equalsIgnoreCase("host")).findFirst().get().getValue().split(":", 2);
            if (data.length >= 1) {
                result.setHost(data[0]);
            }
        }
    }

    private static void setupRequestPort(MitmJavaProxyHttpRequest exchange, Request result) {

        result.setPort(exchange.getMethod().getURI().getPort());
        if (result.getPort() <= 0) {
            var data = Arrays.stream(exchange.getMethod().getAllHeaders()).filter(h->h.getName().equalsIgnoreCase("host")).findFirst().get().getValue().split(":", 2);
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

    public static String[] splitByString(String separator, String toSplit) {
        var result = new String[2];
        var pos = toSplit.toLowerCase(Locale.ROOT).indexOf(separator.toLowerCase(Locale.ROOT));
        if (pos < 0) return result;
        result[0] = toSplit.substring(0, pos);
        result[1] = toSplit.substring(pos + separator.length());
        return result;
    }
    private static void setupOptionalBody(MitmJavaProxyHttpRequest exchange, Request result)
            throws IOException, FileUploadException {
        var headerContentType = result.getHeader(ConstantsHeader.CONTENT_TYPE);

        if (RequestUtils.isMethodWithBody(result)) {

            InputStream clonedInputStream = exchange.getPlayGround();
            clonedInputStream.mark(8192);//this number may depend on the size of the request body
            var data = IOUtils.toByteArray(clonedInputStream);
            clonedInputStream.reset();
            var contentEncoding = "";
            if (null != result.getHeader("content-encoding")) {
                contentEncoding = result.getHeader("content-encoding").toLowerCase(Locale.ROOT);
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
                var boundary = splitByString("boundary=", headerContentType)[1];
                result.setMultipartData(
                        RequestUtils.buildMultipart(data, boundary, result.getHeader(ConstantsHeader.CONTENT_TYPE)));
            } else if (headerContentType != null && headerContentType
                    .toLowerCase(Locale.ROOT)
                    .startsWith("application/x-www-form-urlencoded")) {
                var requestText = new String(data, StandardCharsets.UTF_8);
                result.setPostParameters(RequestUtils.queryToMap(requestText));
            } else if (headerContentType != null && headerContentType
                    .toLowerCase(Locale.ROOT)
                    .startsWith(ConstantsMime.JSON_SMILE)) {
                var requestText = JsonSmile.smileToJSON(data).toPrettyString();
                result.setRequestText(requestText);
            } else {
                if (result.isBinaryRequest()) {
                    result.setRequestBytes(data);
                } else {
                    result.setRequestText(new String(data, StandardCharsets.UTF_8));
                }
            }
        }
    }

    private static void setupAuthHeaders(Request result) {
        var headerAuthorization = result.getHeader(H_AUTHORIZATION);
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

    
    public boolean isMultipart(Request request) {
        var headerContentType = request.getHeader(ConstantsHeader.CONTENT_TYPE);
        if (headerContentType == null) return false;
        return headerContentType.toLowerCase(Locale.ROOT).startsWith("multipart");
    }

    
    public boolean hasBody(Request request) {
        if (request.isBinaryRequest()) {
            return request.getRequestBytes() != null;
        } else {
            return request.getRequestText() != null && !request.getRequestText().isEmpty();
        }
    }

    
    public boolean hasBody(Response request) {
        if (request.isBinaryResponse()) {
            return request.getResponseBytes() != null;
        } else {
            return request.getResponseText() != null && !request.getResponseText().isEmpty();
        }
    }

    
    public Response buildResponse(MitmJavaProxyHttpResponse httpResponseMitm)
            throws IOException {
        var response = new Response();
        var httpResponse = httpResponseMitm.getRawResponse();
        HttpEntity responseEntity = httpResponse.getEntity();

        var brotli = false;
        String contentEncoding = "";
        if (responseEntity != null) {
            InputStream in =new ByteArrayInputStream(httpResponseMitm.getBodyBytes());
            //responseEntity.getContent();

            if (null != responseEntity.getContentEncoding()) {
                contentEncoding = responseEntity.getContentEncoding().getValue().toLowerCase(Locale.ROOT);
            }
            if (contentEncoding == null) contentEncoding = "";


            brotli = contentEncoding.equalsIgnoreCase("br");
            if (responseEntity.getContentType() != null
                    && responseEntity.getContentType().getValue() != null
                    && MimeChecker.isBinary(responseEntity.getContentType().getValue(), contentEncoding)) {

                if (brotli) {
                    response.setResponseBytes(IOUtils.toByteArray(new BrotliInputStream(in)));
                    response.removeHeader("content-encoding");
                } else {
                    response.setResponseBytes(IOUtils.toByteArray(in));
                }
                response.setBinaryResponse(true);
            } else {
                String responseText = null;
                if (brotli) {
                    responseText = IOUtils.toString(new BrotliInputStream(in), StandardCharsets.UTF_8);
                    response.removeHeader("content-encoding");
                } else if (responseEntity.getContentType() != null && responseEntity.getContentType().getValue().equalsIgnoreCase(ConstantsMime.JSON_SMILE)) {
                    responseText = JsonSmile.smileToJSON(IOUtils.toByteArray(in)).toPrettyString();
                } else {
                    responseText = IOUtils.toString(in, StandardCharsets.UTF_8);
                }
                response.setResponseText(responseText);
            }
        } else {
            response.setBinaryResponse(true);
            response.setResponseBytes(new byte[0]);
        }
        response.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        for (var header : httpResponse.getAllHeaders()) {
            if (header.getName().equalsIgnoreCase("transfer-encoding")) continue;
            response.addHeader(header.getName(), header.getValue());
        }
        if (brotli) {
            response.removeHeader("content-encoding");
        }
        return response;
    }


    public Request buildRequest(MitmJavaProxyHttpRequest exchange)
            throws IOException, FileUploadException {
        var result = new Request();

        //result.setRemoteHost(exchange.getRemoteAddress().getAddress().getHostAddress());
        result.setProtocol(exchange.getMethod().getURI().getScheme().toLowerCase(Locale.ROOT));

        result.setQuery(RequestUtils.queryToMap(exchange.getMethod().getURI().getRawQuery()));


        setupRequestHost(exchange, result);
        setupRequestPort(exchange, result);
        result.setPath(exchange.getMethod().getURI().getRawPath());
        result.setMethod(exchange.getMethod().getMethod().toUpperCase(Locale.ROOT));
        result.setHeaders(RequestUtils.headersToMap(exchange.getMethod().getAllHeaders()));
        var headerContentType = result.getHeader(ConstantsHeader.CONTENT_TYPE);

        result.setSoapRequest(result.getHeader(H_SOAP_ACTION) != null);
        setupAuthHeaders(result);

        result.setBinaryRequest(MimeChecker.isBinary(headerContentType, ""));
        result.setStaticRequest(MimeChecker.isStatic(headerContentType, result.getPath()));
        setupOptionalBody(exchange, result);
        // result.sanitizedPath = RequestUtils.sanitizePath(result);
        return result;
    }
}
