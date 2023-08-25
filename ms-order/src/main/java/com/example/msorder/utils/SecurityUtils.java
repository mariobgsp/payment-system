package com.example.msorder.utils;

import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtils {

    public static String encodeRequestBody(String requestBody, String secret) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            hmacSha256.init(secretKey);

            return Base64.encodeBase64String(hmacSha256.doFinal(requestBody.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
