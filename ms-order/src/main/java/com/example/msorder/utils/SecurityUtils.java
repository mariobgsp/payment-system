package com.example.msorder.utils;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SecurityUtils {

    private static final BCryptPasswordEncoder BCRYPT_ENCODER = new BCryptPasswordEncoder();

    public static String encodeRequestBody(String requestBody, String secret) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKey);

            return Base64.encodeBase64String(hmacSha256.doFinal(requestBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verifies a raw password against the stored credential.
     * BCrypt hashes (prefix "$2") are verified with BCrypt; legacy
     * HMAC-SHA256 hashes are verified for backward compatibility.
     */
    public static boolean verifyPassword(String rawPassword, String storedHash, String secret) {
        if (rawPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        if (storedHash.startsWith("$2")) {
            try {
                return BCRYPT_ENCODER.matches(rawPassword, storedHash);
            } catch (Exception e) {
                return false;
            }
        }
        String expected = encodeRequestBody(rawPassword, secret);
        if (expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
