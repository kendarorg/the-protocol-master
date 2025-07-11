package org.kendar.sample;

import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncodeDecodeTest {

    /*public static final byte[] fromHexString(final String s) {
        byte[] arr = new byte[s.length()/2];
        for ( int start = 0; start < s.length(); start += 2 )
        {
            String thisByte = s.substring(start, start+2);
            arr[start/2] = Byte.parseByte(thisByte, 16);
        }
        return arr;
    }*/

    public byte[] fromHexString(String s) {
        final int len = s.length();

        // "111" is not a valid hex encoding.
        if (len % 2 != 0) {
            throw new IllegalArgumentException("hexBinary needs to be even-length: " + s);
        }

        byte[] out = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            int h = hexToBin(s.charAt(i));
            int l = hexToBin(s.charAt(i + 1));
            if (h == -1 || l == -1) {
                throw new IllegalArgumentException("contains illegal character for hexBinary: " + s);
            }

            out[i / 2] = (byte) (h * 16 + l);
        }

        return out;
    }

    private static int hexToBin(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if ('a' <= ch && ch <= 'f') {
            return ch - 'a' + 10;
        }
        return -1;
    }
    @Test
    void encodeDecode() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        var enc = getClass().getResourceAsStream("/0335.enc").readAllBytes();
        var dec = getClass().getResourceAsStream("/0335.ts").readAllBytes();

        var initVector = fromHexString("43A6D967D5C17290D98322F5C8F6660B");
        var key = fromHexString("b20782cbc32a7843e7691d55950c9a6a");
        IvParameterSpec iv = new IvParameterSpec(initVector);
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        byte[] original = cipher.doFinal(enc);
        assertEquals(dec.length, original.length, "Decoded length differs from original");
        for (int i = 0; i < original.length; i++) {
            var o = original[i];
            var d = dec[i];
            assertEquals(o, d, "Byte " + i + " differs. Enc: " + o + " Dec: " + d);

        }
        System.out.println("AAA");
    }
}
