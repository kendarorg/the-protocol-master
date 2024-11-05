package org.kendar.http.utils.utils;


import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Md5Tester {
    public static String calculateMd5(Object data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (data == null) {
                return "0";
            }
            if (data instanceof String) {
                if (((String) data).length() == 0) return "0";
                md.update(((String) data).getBytes(StandardCharsets.UTF_8));
            } else {
                if (((byte[]) data).length == 0) return "0";
                md.update((byte[]) data);
            }
            byte[] digest = md.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            return bigInt.toString(16);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
