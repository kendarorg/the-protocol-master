package org.kendar.http;

import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsExchange;
import org.apache.http.conn.HttpClientConnectionManager;
import org.kendar.http.utils.*;
import org.kendar.http.utils.callexternal.ExternalRequester;
import org.kendar.http.utils.constants.ConstantsHeader;
import org.kendar.http.utils.constants.ConstantsMime;
import org.kendar.http.utils.converters.RequestResponseBuilder;
import org.kendar.http.utils.plugins.PluginClassesHandler;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;


public class MasterHandler implements HttpHandler {

    public static final String BLOCK_RECURSION = "X-BLOCK-RECURSIVE";
    private static final Logger log = LoggerFactory.getLogger("org.kendar.http.Main");
    private final PluginClassesHandler pluginClassesHandler;
    //private final SimpleRewriterHandler simpleProxyHandler;
    private final RequestResponseBuilder requestResponseBuilder;
    private final ExternalRequester externalRequester;
    private final ConnectionBuilder connectionBuilder;

    public MasterHandler(

            PluginClassesHandler pluginClassesHandler,
            //SimpleRewriterHandler simpleProxyHandler,
            RequestResponseBuilder requestResponseBuilder,
            ExternalRequester externalRequester,
            ConnectionBuilder connectionBuilder) {

        this.pluginClassesHandler = pluginClassesHandler;
        //this.simpleProxyHandler = simpleProxyHandler;

        this.requestResponseBuilder = requestResponseBuilder;
        this.externalRequester = externalRequester;
        this.connectionBuilder = connectionBuilder;

    }

    private void sendResponse(Response response, HttpExchange httpExchange) throws IOException {
        byte[] data = new byte[0];
        var dataLength = 0;
        if (requestResponseBuilder.hasBody(response)) {
            if (MimeChecker.isBinary(response) && response.getResponseText() instanceof BinaryNode) {
                data = ((BinaryNode) response.getResponseText()).binaryValue();
            } else if (response.getResponseText() != null) {

                if (ConstantsMime.JSON_SMILE.equalsIgnoreCase(response.getFirstHeader(ConstantsHeader.CONTENT_TYPE))) {
                    data = JsonSmile.jsonToSmile(response.getResponseText());
                } else if (MimeChecker.isJson(response.getFirstHeader(ConstantsHeader.CONTENT_TYPE))) {
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

    private void handleException(HttpExchange httpExchange, Response response, Exception ex) {
        try {
            log.error("ERROR HANDLING HTTP REQUEST ", ex);
            if (response.getHeader(ConstantsHeader.CONTENT_TYPE) == null) {
                response.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.HTML);
            }
            response.addHeader("X-Exception-Type", ex.getClass().getName());
            response.addHeader("X-Exception-Message", ex.getMessage());
            response.addHeader("X-Exception-PrevStatusCode", Integer.toString(response.getStatusCode()));
            response.setStatusCode(500);
            if (!requestResponseBuilder.hasBody(response)) {
                response.setResponseText(new TextNode(ex.getMessage()));
            } else {
                response.setResponseText(new TextNode(ex.getMessage()));
            }
            sendResponse(response, httpExchange);
        } catch (Exception xx) {
            log.trace(xx.getMessage());
        }
    }

    @Override
    public void handle(HttpExchange httpExchange) {

        var connManager = connectionBuilder.getConnectionManger(true, true);
        var pluginContext = new PluginContext("HTTP", httpExchange.getRequestMethod().toUpperCase(), System.currentTimeMillis(), null);
        Request request = null;
        Response response = new Response();
        try {
            if (httpExchange instanceof HttpsExchange) {
                request = requestResponseBuilder.fromExchange(httpExchange, "https");
            } else {
                request = requestResponseBuilder.fromExchange(httpExchange, "http");
            }
            request.removeHeader("transfer-encoding");
            request.setMs(Calendar.getInstance().getTimeInMillis());

            //START REQUEST
            if (request.getHeader(BLOCK_RECURSION) != null) {
                var uri = new URI(request.getFirstHeader(BLOCK_RECURSION));
                if (uri.getHost().equalsIgnoreCase(request.getHost()) &&
                        uri.getPath().equalsIgnoreCase(request.getPath())) {
                    response.addHeader("ERROR", "Recursive call on " + request.getHeader(BLOCK_RECURSION));
                    response.setStatusCode(404);
                    sendResponse(response, httpExchange);
                    return;
                }
            }

            log.info("[SERVER][REQ  ] {} {}", request.getMethod(), request.buildUrl().substring(0, Math.min(request.buildUrl().length(), 60)));

            handleInternal(pluginContext, request, response, connManager);
            sendResponse(response, httpExchange);

        } catch (Exception rex) {
            handleException(httpExchange, response, rex);
            try {
                httpExchange.close();
            } catch (Exception exx) {
            }
        } finally {
            try {
                pluginClassesHandler.handle(
                        pluginContext, ProtocolPhase.FINALIZE, request, response, connManager);

            } catch (Exception e) {
                log.error("ERROR CALLING POST RENDER ", e);
            }
        }
    }

    private void handleInternal(PluginContext pluginContext, Request request, Response response, HttpClientConnectionManager connManager) throws Exception {

        if (pluginClassesHandler.handle(pluginContext,
                ProtocolPhase.CONNECT, request, response, connManager)) {

            return;
        }

        //var proxiedRequest = simpleProxyHandler.translate(request);

        if (pluginClassesHandler.handle(
                pluginContext, ProtocolPhase.PRE_CALL, request, response, connManager)) {
            return;
        }

        externalRequester.callSite(request, response);

        pluginClassesHandler.handle(
                pluginContext, ProtocolPhase.POST_CALL, request, response, connManager);
    }
}
