package org.kendar.mongo;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.SynchronousQueue;

import org.json.JSONObject;

public class MongoAuthHandlerAll {

    private final SynchronousQueue<JSONObject> inbound;
    private final SynchronousQueue<JSONObject> outbound;

    // SCRAM material (password-equivalent)
    private final byte[] salt;
    private final int iterations;
    private final byte[] storedKey;
    private final byte[] serverKey;

    private String mechanism;
    private String clientFirstBare;
    private String serverFirst;
    private int conversationId = 1;

    public MongoAuthHandlerAll(
            SynchronousQueue<JSONObject> inbound,
            SynchronousQueue<JSONObject> outbound,
            byte[] salt,
            int iterations,
            byte[] storedKey,
            byte[] serverKey
    ) {
        this.inbound = inbound;
        this.outbound = outbound;
        this.salt = salt;
        this.iterations = iterations;
        this.storedKey = storedKey;
        this.serverKey = serverKey;
    }

    public void run() throws Exception {
        while (true) {
            JSONObject cmd = inbound.take();

            if (cmd.has("saslStart")) {
                handleSaslStart(cmd);
            } else if (cmd.has("saslContinue")) {
                handleSaslContinue(cmd);
            } else {
                throw new IllegalStateException("Unexpected command during auth");
            }
        }
    }

    private void handleSaslStart(JSONObject cmd) throws Exception {
        mechanism = cmd.getString("mechanism");

        switch (mechanism) {
            case "SCRAM-SHA-256":
            case "SCRAM-SHA-1":
                handleScramStart(cmd);
                break;
            case "MONGODB-X509":
                outbound.put(success());
                break;
            case "PLAIN":
                handlePlainStart(cmd);
                break;
            case "GSSAPI":
                // Accept any token for stub purposes
                outbound.put(success());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported mechanism: " + mechanism);
        }
    }

    private void handleSaslContinue(JSONObject cmd) throws Exception {
        switch (mechanism) {
            case "SCRAM-SHA-256":
            case "SCRAM-SHA-1":
                handleScramContinue(cmd);
                break;
            case "PLAIN":
            case "MONGODB-X509":
            case "GSSAPI":
                // Usually done in start
                outbound.put(success());
                break;
            default:
                throw new UnsupportedOperationException("Unexpected saslContinue for: " + mechanism);
        }
    }

    // ===== SCRAM =====
    private void handleScramStart(JSONObject cmd) throws Exception {
        byte[] payload = Base64.getDecoder().decode(cmd.getString("payload"));
        clientFirstBare = new String(payload, StandardCharsets.UTF_8).substring(3);
        String serverNonce = extractNonce(clientFirstBare) + randomNonce();
        serverFirst = "r=" + serverNonce +
                ",s=" + Base64.getEncoder().encodeToString(salt) +
                ",i=" + iterations;

        JSONObject reply = new JSONObject()
                .put("conversationId", conversationId)
                .put("done", false)
                .put("payload", Base64.getEncoder()
                        .encodeToString(serverFirst.getBytes(StandardCharsets.UTF_8)))
                .put("ok", 1);

        outbound.put(reply);
    }

    private void handleScramContinue(JSONObject cmd) throws Exception {
        byte[] payload = Base64.getDecoder().decode(cmd.getString("payload"));
        String clientFinal = new String(payload, StandardCharsets.UTF_8);

        String authMessage =
                clientFirstBare + "," +
                        serverFirst + "," +
                        clientFinal.substring(0, clientFinal.indexOf(",p="));

        byte[] clientSignature = hmac(storedKey, authMessage);
        byte[] clientProof = xor(
                Base64.getDecoder().decode(extractProof(clientFinal)),
                clientSignature
        );

        byte[] computedStoredKey = sha(hash(clientProof));

        if (!MessageDigest.isEqual(computedStoredKey, storedKey)) {
            throw new SecurityException("Invalid SCRAM proof");
        }

        String serverSignature = Base64.getEncoder()
                .encodeToString(hmac(serverKey, authMessage));

        JSONObject reply = new JSONObject()
                .put("conversationId", conversationId)
                .put("done", true)
                .put("payload", Base64.getEncoder()
                        .encodeToString(("v=" + serverSignature)
                                .getBytes(StandardCharsets.UTF_8)))
                .put("ok", 1);

        outbound.put(reply);
    }

    // ===== PLAIN =====
    private void handlePlainStart(JSONObject cmd) throws Exception {
        byte[] payload = Base64.getDecoder().decode(cmd.getString("payload"));
        String decoded = new String(payload, StandardCharsets.UTF_8);
        // Format: \0username\0password
        String[] parts = decoded.split("\0");
        if (parts.length < 3) throw new SecurityException("Invalid PLAIN payload");
        String password = parts[2];
        // Compare with known password (simple)
        if (!verifyPassword(password)) throw new SecurityException("Invalid PLAIN password");

        outbound.put(success());
    }

    private boolean verifyPassword(String password) {
        // For testing, compare with fixed password
        return "secret".equals(password);
    }

    private JSONObject success() {
        return new JSONObject()
                .put("conversationId", conversationId)
                .put("done", true)
                .put("ok", 1);
    }

    // ===== Helpers =====
    private static byte[] hmac(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sha(byte[] in) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(in);
    }

    private static byte[] hash(byte[] in) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(in);
    }

    private static byte[] xor(byte[] a, byte[] b) {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) out[i] = (byte) (a[i] ^ b[i]);
        return out;
    }

    private static String extractNonce(String msg) {
        for (String p : msg.split(",")) {
            if (p.startsWith("r=")) return p.substring(2);
        }
        throw new IllegalArgumentException("No nonce");
    }

    private static String extractProof(String msg) {
        for (String p : msg.split(",")) {
            if (p.startsWith("p=")) return p.substring(2);
        }
        throw new IllegalArgumentException("No proof");
    }

    private static String randomNonce() {
        return Long.toHexString(Double.doubleToLongBits(Math.random()));
    }
}
