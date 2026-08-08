package com.example.msorder.service;

import com.example.msorder.config.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    @Autowired
    private AppProperties appProperties;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public record Session(String token, String userId, String username, long createdAt) {}

    public String create(String userId, String username) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(token, userId, username, System.currentTimeMillis()));
        return token;
    }

    public Session validate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        long ttlMillis = appProperties.getSESSION_TTL_SECONDS() * 1000;
        if (System.currentTimeMillis() - session.createdAt() > ttlMillis) {
            sessions.remove(token);
            return null;
        }
        return session;
    }

    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
