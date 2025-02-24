package org.kendar.http.utils.callexternal;

import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.converters.MultipartPart;
import org.kendar.apis.converters.RequestResponseBuilder;
import org.kendar.apis.converters.RequestUtils;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.apis.utils.JsonSmile;
import org.kendar.apis.utils.MimeChecker;
import org.kendar.http.utils.ConnectionBuilder;
import org.kendar.http.utils.dns.DnsMultiResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("resource")
public abstract class BaseRequesterImpl implements BaseRequester {
    public static final String BLOCK_RECURSION = "X-BLOCK-RECURSIVE";
    private static final Logger log = LoggerFactory.getLogger(BaseRequesterImpl.class);
    private static final HttpRequestRetryHandler requestRetryHandler =
            (exception, executionCount, context) -> executionCount != 1;
    protected final DnsMultiResolver multiResolver;
    private final RequestResponseBuilder requestResponseBuilder;
    private final ConnectionBuilder connectionBuilder;

    public BaseRequesterImpl(RequestResponseBuilder requestResponseBuilder,
                             DnsMultiResolver multiResolver,
                             ConnectionBuilder connectionBuilder) {
        this.requestResponseBuilder = requestResponseBuilder;

        this.multiResolver = multiResolver;
        this.connectionBuilder = connectionBuilder;
    }

    public void callSite(Request request, Response response)
            throws Exception {

        var contentEncoding = "";
        if (null != request.getHeader(ConstantsHeader.CONTENT_ENCODING)) {
            var firstHeader = request.getFirstHeader(ConstantsHeader.CONTENT_ENCODING);
            if (firstHeader != null) {
                contentEncoding = firstHeader.toLowerCase(Locale.ROOT);
            }
        }
        var brotli = contentEncoding.equalsIgnoreCase("br");
        var gzip = contentEncoding.equalsIgnoreCase("gzip");


        if (request.getHeader(BLOCK_RECURSION) != null) {
            response.setStatusCode(500);
            return;
        }

        CloseableHttpClient httpClient;
        if (request.getHost().equalsIgnoreCase("127.0.0.1") ||
                request.getHost().equalsIgnoreCase("localhost")) {
            final SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                    .build();
            httpClient = HttpClientBuilder.create().
                    disableAutomaticRetries().
                    setSSLContext(sslContext).
                    setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).
                    disableRedirectHandling().
                    build();
        } else {
            httpClient = connectionBuilder.buildClient(true, true, request.getPort(), request.getProtocol());
        }

        HttpRequestBase fullRequest = null;
        try {
            String fullAddress = RequestUtils.buildFullAddress(request, true);
            fullRequest = createFullRequest(request, fullAddress);
            fullRequest.addHeader(BLOCK_RECURSION, fullAddress);
            if (request.getHeaders() != null) {
                for (var header : request.getHeaders().entrySet()) {
                    if (!header.getKey().equalsIgnoreCase("host")
                            && !header.getKey().equalsIgnoreCase(ConstantsHeader.CONTENT_LENGTH)) {
                        for (var item : header.getValue()) {
                            fullRequest.addHeader(header.getKey(), item);
                        }
                    }
                }
            }
            var port = request.getPort();
            if (port > 0) {
                fullRequest.addHeader("Host", request.getHost() + ":" + port);
            } else {
                fullRequest.addHeader("Host", request.getHost());
            }
            //MAIN_TODO HANDLE SOAP REQUEST
            /*if (request.isSoapRequest()) {
                HttpEntity entity = handleSoapRequest(request);
                ((HttpEntityEnclosingRequestBase) fullRequest).setEntity(entity);
            } else */
            if (!request.getPostParameters().isEmpty()) {
                List<NameValuePair> form = new ArrayList<>();
                for (var par : request.getPostParameters().entrySet()) {
                    form.add(new BasicNameValuePair(par.getKey(), par.getValue()));
                }
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);

                if (gzip) {
                    ((HttpEntityEnclosingRequestBase) fullRequest).setEntity(new GzipCompressingEntity(entity));
                } else {
                    ((HttpEntityEnclosingRequestBase) fullRequest).setEntity(entity);
                }
            } else if (requestResponseBuilder.isMultipart(request)) {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                for (MultipartPart part : request.getMultipartData()) {
                    if (MimeChecker.isBinary(part.getContentType(), null)) {
                        var cb = new ByteArrayBody(part.getByteData(), part.getFileName());
                        var fbd = new FormBodyPart(part.getFieldName(), cb);
                        for (var header : part.getHeaders().entrySet()) {
                            if (null == fbd.getHeader().getField(header.getKey())) {
                                fbd.addField(header.getKey(), header.getValue());
                            }
                        }
                        builder.addPart(fbd);
                    } else {
                        var cb = new StringBody(part.getStringData());
                        var fbd = new FormBodyPart(part.getFieldName(), cb);
                        for (var header : part.getHeaders().entrySet()) {
                            if (null == fbd.getHeader().getField(header.getKey())) {
                                fbd.addField(header.getKey(), header.getValue());
                            }
                        }
                        builder.addPart(fbd);
                    }
                }

                HttpEntity entity = builder.build();

                if (gzip) {
                    ((HttpEntityEnclosingRequestBase) fullRequest).setEntity(new GzipCompressingEntity(entity));
                } else {
                    ((HttpEntityEnclosingRequestBase) fullRequest).setEntity(entity);
                }
            } else if (requestResponseBuilder.hasBody(request)) {
                HttpEntity entity;
                try {
                    String contentType = request.getFirstHeader(ConstantsHeader.CONTENT_TYPE);
                    if (contentType == null) {
                        contentType = ConstantsMime.DEFAULT_CONTENT_TYPE;
                    }
                    if (contentType.indexOf(";") > 0) {
                        var spl = contentType.split(";");
                        contentType = spl[0];
                    }
                    if (ConstantsMime.JSON_SMILE.equalsIgnoreCase(contentType)) {
                        entity =
                                new ByteArrayEntity(
                                        JsonSmile.jsonToSmile(request.getRequestText()), ContentType.create(contentType));
                    } else if (MimeChecker.isBinary(request)) {
                        entity =
                                new ByteArrayEntity(
                                        ((BinaryNode) request.getRequestText()).binaryValue(), ContentType.create(contentType));

                    } else {
                        entity =
                                new StringEntity(
                                        request.getRequestText().toString(), ContentType.create(contentType));
                    }
                } catch (Exception ex) {
                    log.debug("Error creating request {} {}", request.buildUrl(), request.getHeader(ConstantsHeader.CONTENT_TYPE), ex);
                    entity =
                            new StringEntity(
                                    request.getRequestText().toString(), ContentType.create(ConstantsMime.STREAM));
                }
                if (gzip) {
                    ((HttpEntityEnclosingRequestBase) fullRequest).setEntity(new GzipCompressingEntity(entity));
                } else {
                    ((HttpEntityEnclosingRequestBase) fullRequest).setEntity(entity);
                }
            }

            HttpResponse httpResponse = null;
            try {
                httpResponse = httpClient.execute(fullRequest);
                requestResponseBuilder.fromHttpResponse(httpResponse, response);
            } catch (Exception ex) {
                response.setStatusCode(404);
                response.getHeaders().put("Content-Type", List.of("text/plain"));
                response.setResponseText(new TextNode(ex.getMessage()));
                if (httpResponse != null) {
                    response.setStatusCode(httpResponse.getStatusLine().getStatusCode());
                    response.setResponseText(new TextNode(httpResponse.getStatusLine().getReasonPhrase() + " " + ex.getMessage()));
                }
            }
        } finally {
            if (fullRequest != null) {
                fullRequest.releaseConnection();
            }
        }
    }


    private HttpRequestBase createFullRequest(Request request, String stringAdress) throws Exception {
        //var partialAddress= new URI(stringAdress).toString();
        //.skip(3).collect(Collectors.toList()));

        //var fullAddress =
        if (request.getMethod().equalsIgnoreCase("POST")) {
            return new HttpPost(stringAdress);
        } else if (request.getMethod().equalsIgnoreCase("PUT")) {
            return new HttpPut(stringAdress);
        } else if (request.getMethod().equalsIgnoreCase("PATCH")) {
            return new HttpPatch(stringAdress);
        } else if (request.getMethod().equalsIgnoreCase("GET")) {
            return new HttpGet(stringAdress);
        } else if (request.getMethod().equalsIgnoreCase("DELETE")) {
            return new HttpDelete(stringAdress);
        } else if (request.getMethod().equalsIgnoreCase("HEAD")) {
            return new HttpHead(stringAdress);
        } else if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            return new HttpOptions(stringAdress);
        } else if (request.getMethod().equalsIgnoreCase("TRACE")) {
            return new HttpTrace(stringAdress);
        } else {
            log.error("Missing http method {} on {}", request.getMethod(), request.buildUrl());
            throw new Exception("Missing http method " + request.getMethod() + " on " + request.buildUrl());
        }
    }
}
