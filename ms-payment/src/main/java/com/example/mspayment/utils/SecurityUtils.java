package com.example.mspayment.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SecurityUtils {

    private static final long CALLBACK_TOLERANCE_SECONDS = 300;

    public static boolean verifyCallbackSignature(String secret, String timestamp, String body, String signature) {
        if (secret == null || secret.isEmpty()
                || timestamp == null || timestamp.isEmpty()
                || body == null || signature == null || signature.isEmpty()) {
            return false;
        }

        // replay protection: reject timestamps older than 5 minutes
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - ts) > CALLBACK_TOLERANCE_SECONDS) {
            return false;
        }

        String expected = hmacSha256Hex(secret, timestamp, body);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    public static String hmacSha256Hex(String secret, String timestamp, String payload) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKey);
            hmacSha256.update(timestamp.getBytes(StandardCharsets.UTF_8));
            byte[] digest = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
