package org.kendar.http;

import net.lightbody.bmp.proxy.util.CappedByteArrayOutputStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.http.client.methods.HttpRequestBase;
import org.kendar.http.data.Request;
import org.kendar.http.data.RequestResponseBuilderImpl;
import org.kendar.http.data.Response;
import org.kendar.http.rewrite.CustomResponderInterceptor;
import org.kendar.utils.JsonMapper;
import website.magyar.mitm.proxy.RequestInterceptor;
import website.magyar.mitm.proxy.ResponseInterceptor;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpRequest;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TpmInterceptor implements RequestInterceptor, ResponseInterceptor, CustomResponderInterceptor {
    private final RequestResponseBuilderImpl requestResponseBuilder;
    private ConcurrentHashMap<String, Request> requests = new ConcurrentHashMap<>();
    private static AtomicInteger counter = new AtomicInteger(0);
    public TpmInterceptor( RequestResponseBuilderImpl requestResponseBuilder) {

        this.requestResponseBuilder = requestResponseBuilder;
    }

    @Override
    public void process(MitmJavaProxyHttpRequest mitmJavaProxyHttpRequest) {
        try {
            var request = requestResponseBuilder.buildRequest(mitmJavaProxyHttpRequest);
            request.setId(counter.incrementAndGet());
            System.out.println("REQ"+request.getId()+": " + request.getHost()+request.getPath());
            requests.put(mitmJavaProxyHttpRequest.getMessageId(),request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (FileUploadException e) {
            throw new RuntimeException(e);
        }

    }

    private static final ExecutorService executorService = Executors.newFixedThreadPool(20);
    JsonMapper mapper = new JsonMapper();

    @Override
    public void process(MitmJavaProxyHttpResponse mitmJavaProxyHttpResponse) {
        try {

            var response = requestResponseBuilder.buildResponse(mitmJavaProxyHttpResponse);
            var mitmId= mitmJavaProxyHttpResponse.getEntry().getMessageId();
            var request = requests.get(mitmJavaProxyHttpResponse.getEntry().getMessageId());
            System.out.println("RES"+request.getId()+": " + request.getHost()+request.getPath());
            executorService.submit(()->{
                var roundtrip = new RoundTrip(request,response);
                try {
                    Files.writeString(Path.of("protocol-http","target","http"+request.getId()+".json"),mapper.serialize(roundtrip));

                    System.out.println("WRI"+request.getId()+": " + request.getHost()+request.getPath());
                    requests.remove(mitmId);
                } catch (IOException e) {
                    requests.remove(mitmId);
                    System.out.println("ERR"+request.getId()+": " + request.getHost()+request.getPath());
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final int MAX_BUFFER_SIZE = 1024 * 1024;
    @Override
    public MitmJavaProxyHttpResponse intercept(MitmJavaProxyHttpRequest req) {
        var resp = new Response();
        resp.setStatusCode(200);
        resp.addHeader("Content-Type", "application/json");
        resp.setResponseText("{}");
        ByteArrayOutputStream baos =new ByteArrayOutputStream(0);;
        String text = null;
        if(resp.isBinaryResponse()) {
            byte[] bytes = resp.getResponseBytes();
            baos = new ByteArrayOutputStream(bytes.length);
            baos.writeBytes(bytes);
            baos.reset();
        }else{
            byte[] bytes = resp.getResponseText().getBytes(StandardCharsets.UTF_8);
            baos = new ByteArrayOutputStream(bytes.length);
            baos.writeBytes(bytes);
            baos.reset();
        }

        var os = new CappedByteArrayOutputStream(MAX_BUFFER_SIZE);
        HttpRequestBase method = req.getMethod();
        return new MitmJavaProxyHttpResponse(resp.getStatusCode(), null, method,
                req.getProxyRequest().getURI(), null, "",
                resp.getResponseText(),  resp.getHeader("Content-Type"), "UTF-8",
                baos, os, true);
    }
}
