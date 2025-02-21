package org.kendar.utils;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.PaddedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

// A simple example that uses the Bouncy Castle
// lightweight cryptography API to perform DES
// encryption of arbitrary data.


public class Encryptor {

    private final BufferedBlockCipher cipher;
    private final KeyParameter key;


    // Initialize the cryptographic engine.
    // The key array should be at least 8 bytes long.


    public Encryptor(byte[] key) {
            /*
            cipher = new PaddedBlockCipher(
                       new CBCBlockCipher(new DESEngine()));
            */
        cipher = new PaddedBlockCipher(
                new CBCBlockCipher(new BlowfishEngine()));
        this.key = new KeyParameter(key);
    }

    // Initialize the cryptographic engine.
    // The string should be at least 8 chars long.

    public Encryptor(String key) {
        this(key.getBytes());
    }
    // Private routine that does the gritty work.

    private byte[] callCipher(byte[] data)
            throws CryptoException {
        int size = cipher.getOutputSize(data.length);

        byte[] result = new byte[size];
        int olen = cipher.processBytes(data, 0, data.length, result, 0);
        olen += cipher.doFinal(result, olen);

        if (olen < size) {
            byte[] tmp = new byte[olen];
            System.arraycopy(
                    result, 0, tmp, 0, olen);
            result = tmp;
        }

        return result;
    }
    // Encrypt arbitrary byte array, returning the
    // encrypted data in a different byte array.

    public synchronized byte[] encrypt(byte[] data)
            throws CryptoException {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        cipher.init(true, key);
        return callCipher(data);
    }
    // Encrypts a string.

    public byte[] encryptString(String data)
            throws CryptoException {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }

        return encrypt(data.getBytes());
    }
    // Decrypts arbitrary data.

    public synchronized byte[] decrypt(byte[] data)
            throws CryptoException {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        cipher.init(false, key);
        return callCipher(data);
    }
    // Decrypts a string that was previously encoded
    // using encryptString.

    public String decryptString(byte[] data)
            throws CryptoException {
        if (data == null || data.length == 0) {
            return "";
        }

        return new String(decrypt(data));
    }
}
