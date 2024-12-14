package org.kendar.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class ApiTestBase extends BasicTest {

    public static <T> T getRequest(String target, CloseableHttpClient httpclient, TypeReference<T> typeReference) throws IOException {
        var httpget = new HttpGet(target);
        var httpresponse = httpclient.execute(httpget);

        var sc = new Scanner(httpresponse.getEntity().getContent());
        var result = "";
        while (sc.hasNext()) {
            result += (sc.nextLine());
        }
        System.out.println(result);
        return mapper.deserialize(result, typeReference);
    }

    public static byte[] downloadRequest(String target, CloseableHttpClient httpclient) throws IOException {
        var httpget = new HttpGet(target);
        var httpresponse = httpclient.execute(httpget);

        var baos = new ByteArrayOutputStream();
        httpresponse.getEntity().writeTo(baos);
        return baos.toByteArray();
    }

    public static String downloadRequestString(String target, CloseableHttpClient httpclient) throws IOException {
        var bytes = downloadRequest(target, httpclient);
        return new String(bytes);
    }

    public static <T> T postRequest(String target, CloseableHttpClient httpclient, byte[] data, TypeReference<T> typeReference) throws IOException {
        var httpget = new HttpPost(target);
        var be = new ByteArrayEntity(data);
        httpget.setEntity(be);
        var httpresponse = httpclient.execute(httpget);

        var sc = new Scanner(httpresponse.getEntity().getContent());
        var result = "";
        while (sc.hasNext()) {
            result += (sc.nextLine());
        }
        return mapper.deserialize(result, typeReference);
    }
}
