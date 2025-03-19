package org.kendar.apis;

import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.kendar.apis.base.Response;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.utils.JsonMapper;

public class ApiUtils {
    private static final JsonMapper mapper = new JsonMapper();

    public static void respondOk(Response resp) {
        respondJson(resp, new Ok());
    }

    public static void respondKo(Response resp, String error) {
        resp.setStatusCode(500);
        respondJson(resp, new Ko(error));
    }

    public static void respondKo(Response resp, String error,int code) {
        resp.setStatusCode(code);
        respondJson(resp, new Ko(error));
    }


    public static void respondKo(Response resp, Exception error) {

        respondKo(resp, error.getMessage());
    }

    public static void respondJson(Response resp, Object toSerialiez) {
        resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
        if(toSerialiez instanceof String) {
            resp.setResponseText(new TextNode((String)toSerialiez));
        }else {
            resp.setResponseText(mapper.toJsonNode(toSerialiez));
        }
    }


    public static void respondText(Response resp, String data) {
        resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.TEXT);
        resp.setResponseText(new TextNode(data));
    }

    public static void respondFile(Response resp, byte[] data, String contentType, String name) {
        resp.addHeader(ConstantsHeader.CONTENT_TYPE, contentType);
        resp.addHeader("Content-Transfer-Encoding", "binary");
        resp.addHeader("Content-Disposition", "attachment; filename=\"" + name + "\";");
        resp.setResponseText(new BinaryNode(data));
    }
}
