package org.kendar.http.utils.converters;

import com.sun.net.httpserver.Headers;
import org.apache.commons.fileupload.FileUploadException;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.constants.ConstantsMime;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

public class RequestUtils {
    public static boolean isMethodWithBody(Request result) {
        return result.getMethod().equalsIgnoreCase("POST")
                || result.getMethod().equalsIgnoreCase("PUT")
                || result.getMethod().equalsIgnoreCase("PATCH");
    }

    public static String getFromMap(Map<String, String> map, String index) {
        if (map == null) return null;
        if (map.containsKey(index)) {
            return map.get(index);
        }
        for (var entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(index)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static List<String> getFromMapList(Map<String, List<String>> map, String index) {
        if (map == null) return null;
        if (map.containsKey(index)) {
            return map.get(index);
        }
        for (var entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(index)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static void addToMap(Map<String, String> map, String key, String value) {
        for (var entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                map.put(entry.getKey(), value);
                return;
            }
        }
        map.put(key, value);
    }

    public static void addToMapList(Map<String, List<String>> map, String key, String value) {
        for (var entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                var possible = map.get(entry.getKey());
                if (possible.stream().noneMatch(p -> p.equalsIgnoreCase(value))) {
                    possible.add(value);
                }
                return;
            }
        }
        var partial = new ArrayList<String>();
        partial.add(value);
        map.put(key, partial);
    }

    public static String removeFromMap(Map<String, String> map, String index) {
        if (map.containsKey(index)) {
            String data = map.get(index);
            map.remove(index);
            return data;
        }
        return null;
    }

    public static Map<String, String> queryToMap(String qs) {
        Map<String, String> result = new HashMap<>();
        if (qs == null) return result;

        int last = 0, next, l = qs.length();
        while (last < l) {
            next = qs.indexOf('&', last);
            if (next == -1) next = l;

            if (next > last) {
                int eqPos = qs.indexOf('=', last);
                if (eqPos < 0 || eqPos > next)
                    result.put(URLDecoder.decode(qs.substring(last, next), UTF_8), "");
                else
                    result.put(
                            URLDecoder.decode(qs.substring(last, eqPos), UTF_8),
                            URLDecoder.decode(qs.substring(eqPos + 1, next), UTF_8));
            }
            last = next + 1;
        }
        return result;
    }

    public static Map<String, List<String>> headersToMap(Headers requestHeaders) {
        var result = new HashMap<String, List<String>>();
        for (var entry : requestHeaders.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                result.put(entry.getKey(), new ArrayList<>());
            } else {

                result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }

        return result;
    }

    public static Map<String, String> parseContentDisposition(String value) {
        var result = new HashMap<String, String>();
        var cd = ContentDisposition.parse(value);

        result.put("charset", cd.getCharset());
        result.put("filename", cd.getFilename() == null ? "file" : cd.getFilename());
        result.put("name", cd.getName() == null ? "file" : cd.getName());
        result.put("type", cd.getType() == null ? ConstantsMime.STREAM : cd.getType());
        return result;
    }

    private static String byteListToString(List<Byte> l, Charset charset) {
        if (l == null) {
            return "";
        }
        byte[] array = new byte[l.size()];
        int i = 0;
        for (Byte current : l) {
            array[i] = current;
            i++;
        }
        return new String(array, charset);
    }

    public static String sanitizePath(Request result) {
        return result.getHost() + result.getPath();
    }

    public static List<MultipartPart> buildMultipart(byte[] body, String boundary, String contentType)
            throws FileUploadException {
        var blocks = new ArrayList<SimpleBlock>();

        var block = new SimpleBlock();
        var bodyData = new ArrayList<Byte>();
        var test = new ArrayList<Byte>();
        for (var z = 0; z < body.length; z++) {
            if (body[z] == '\r') break;
            test.add(body[z]);
        }
        var fullBoundary = toPrimitive(test);

        for (var i = 0; i < body.length; i++) {
            var fullBoundaryOffest = 0;
            for (; i < body.length && fullBoundaryOffest < fullBoundary.length; i++, fullBoundaryOffest++) {
                if (body[i] != fullBoundary[fullBoundaryOffest]) {
                    throw new FileUploadException();
                }
            }
            if (body[i] == '\r' && body[i + 1] == '\n') {
                i += 2;
            } else if (body[i] == '-' && body[i + 1] == '-') {
                break;
            }
            //i++;i++;
            block = new SimpleBlock();
            var firstHeader = new ArrayList<Byte>();
            for (; i < body.length; i++) {
                if (body[i] == '\r' && body[i + 1] == '\n') {
                    var data = (byte[]) toPrimitive(firstHeader);
                    var hh = new String(data).trim().split(":", 2);
                    block.headers.put(hh[0], hh[1]);
                    firstHeader.clear();
                    if (body[i + 2] == '\r' && body[i + 3] == '\n') {
                        break;
                    }
                } else {
                    firstHeader.add(body[i]);
                }
            }
            i += 4;
            bodyData = new ArrayList<>();
            for (; i < body.length; i++) {
                if (body[i] == '-' && body[i + 1] == '-') {
                    //Check for boundary
                    fullBoundaryOffest = 0;
                    var foundedBoundary = true;
                    var j = i;
                    for (; j < body.length && fullBoundaryOffest < fullBoundary.length; j++, fullBoundaryOffest++) {
                        if (body[j] != fullBoundary[fullBoundaryOffest]) {
                            foundedBoundary = false;
                            break;
                        }
                    }
                    if (foundedBoundary) {
                        if (body[j] == '\r' && body[j + 1] == '\n') {
                            i--;
                        }
                        block.data = toPrimitive(bodyData.subList(0, bodyData.size() - 2));
                        blocks.add(block);
                        //i--;
                        bodyData = new ArrayList<>();
                        block = new SimpleBlock();
                        if (body[j] == '-' && body[j + 1] == '-') {
                            i = body.length;
                        }
                        break;
                    }
                }
                bodyData.add(body[i]);
            }
        }

//        Charset encoding = UTF_8;
//        RequestContext requestContext = new SimpleRequestContext(encoding, contentType, body);
//        FileUploadBase fileUploadBase = new PortletFileUpload();
//        FileItemFactory fileItemFactory = new DiskFileItemFactory();
//        fileUploadBase.setFileItemFactory(fileItemFactory);
//        fileUploadBase.setHeaderEncoding(encoding.displayName());
//        List<FileItem> fileItems = fileUploadBase.parseRequest(requestContext);


        List<MultipartPart> result = new ArrayList<>();
        for (var simpleBlock : blocks) {
            result.add(new MultipartPart(simpleBlock));
        }
        return result;
    }

    private static byte[] toPrimitive(List<Byte> firstHeader) {
        var result = new byte[firstHeader.size()];
        for (var i = 0; i < firstHeader.size(); i++) {
            result[i] = firstHeader.get(i);
        }
        return result;
    }

    public static String buildFullAddress(Request request, boolean usePort) {

        String port = "";

        if (usePort) {
            if (request.getPort() != -1) {
                if (request.getPort() != 443 && request.getProtocol().equalsIgnoreCase("https")) {
                    port = ":" + request.getPort();
                }

                if (request.getPort() != 80 && request.getProtocol().equalsIgnoreCase("http")) {
                    port = ":" + request.getPort();
                }
            }
        }
        return request.getProtocol()
                + "://"
                + request.getHost()
                + port
                + request.getPath()
                + buildFullQuery(request);
    }

    public static String buildFullQuery(Request request) {
        if (request.getQuery().isEmpty()) return "";
        return "?"
                + request.getQuery().entrySet().stream()
                .map(
                        e ->
                                e.getKey()
                                        + "="
                                        + java.net.URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
                                        .replace(" ", "%20"))
                .collect(joining("&"));
    }

    public static class SimpleBlock {
        public final Map<String, String> headers = new HashMap<>();
        public byte[] data = new byte[]{};
    }
}
