package com.example.msorder.delivery;

import com.example.msorder.config.properties.AppProperties;
import com.example.msorder.exception.definition.BadRequestException;
import com.example.msorder.exception.definition.CommonException;
import com.example.msorder.exception.definition.UserNotFoundException;
import com.example.msorder.model.repository.StoreUser;
import com.example.msorder.model.rqrs.request.RequestInfo;
import com.example.msorder.model.rqrs.request.auth.LoginRq;
import com.example.msorder.model.rqrs.response.ResponseInfo;
import com.example.msorder.model.rqrs.response.UserDetailRs;
import com.example.msorder.repository.StoreRepository;
import com.example.msorder.service.SessionService;
import com.example.msorder.utils.CommonUtils;
import com.example.msorder.utils.ResponseUtils;
import com.example.msorder.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/ms/api/v1/auth")
@Slf4j
public class AuthController {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 15 * 60 * 1000;

    private final ConcurrentHashMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private AppProperties appProperties;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRq loginRq,
                                   @RequestHeader(value = "x-request-channel", required = false, defaultValue = "WEB") String channel,
                                   @RequestHeader(value = "x-request-id", required = false) String requestId,
                                   HttpServletRequest httpServletRequest) {
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "auth-login", requestId, null, httpServletRequest);
        ResponseInfo<Object> responseInfo;

        try {
            String clientKey = loginRq.getUsername() != null ? loginRq.getUsername() : "";
            String clientIp = httpServletRequest.getRemoteAddr();
            if (!allowAttempt(clientKey + "|" + clientIp)) {
                throw new BadRequestException("40", "Too many login attempts, try again later");
            }
            if (loginRq == null || StringUtils.isEmpty(loginRq.getUsername()) || StringUtils.isEmpty(loginRq.getPassword())) {
                throw new BadRequestException("03", "Invalid value should not be empty");
            }

            List<StoreUser> storeUsers = storeRepository.getUserDetail(loginRq.getUsername());
            if (storeUsers.isEmpty()) {
                recordFailure(clientKey + "|" + clientIp);
                throw new UserNotFoundException("01", "Invalid username or password");
            }
            StoreUser storeUser = storeUsers.get(0);

            if (!SecurityUtils.verifyPassword(loginRq.getPassword(), storeUser.getHashPassword(), appProperties.getSECRET_KEY())) {
                recordFailure(clientKey + "|" + clientIp);
                throw new UserNotFoundException("01", "Invalid username or password");
            }

            String token = sessionService.create(storeUser.getUserId(), storeUser.getUserName());

            UserDetailRs userDetailRs = new UserDetailRs()
                    .setId(storeUser.getId())
                    .setUserId(storeUser.getUserId())
                    .setUsername(storeUser.getUserName())
                    .setFirstName(storeUser.getFirstName())
                    .setLastName(storeUser.getLastName())
                    .setEmail(storeUser.getEmail())
                    .setSpecialProduct(storeUser.isSpecialProduct())
                    .setRecurring(storeUser.isRecurring())
                    .setToken(token);

            responseInfo = ResponseUtils.generateSuccessRs(request, userDetailRs);
        } catch (Exception e) {
            log.error("[{} - login][Error: {}]", request.getRequestId(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(request, ex);
        }
        return new ResponseEntity<>(responseInfo.getBody(), responseInfo.getHttpHeaders(), responseInfo.getHttpStatus());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestHeader(value = "x-request-channel", required = false, defaultValue = "WEB") String channel,
                                    @RequestHeader(value = "x-request-id", required = false) String requestId,
                                    HttpServletRequest httpServletRequest) {
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "auth-logout", requestId, null, httpServletRequest);
        ResponseInfo<Object> responseInfo;

        try {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                sessionService.revoke(authorization.substring(7).trim());
            }
            responseInfo = ResponseUtils.generateMessageSuccessRs(request, "logged-out");
        } catch (Exception e) {
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(request, ex);
        }
        return new ResponseEntity<>(responseInfo.getBody(), responseInfo.getHttpHeaders(), responseInfo.getHttpStatus());
    }

    private boolean allowAttempt(String key) {
        LoginAttempt attempt = loginAttempts.get(key);
        if (attempt == null) {
            return true;
        }
        if (System.currentTimeMillis() - attempt.firstAttemptAt > WINDOW_MS) {
            loginAttempts.remove(key);
            return true;
        }
        return attempt.count < MAX_ATTEMPTS;
    }

    private void recordFailure(String key) {
        loginAttempts.compute(key, (k, attempt) -> {
            if (attempt == null || System.currentTimeMillis() - attempt.firstAttemptAt > WINDOW_MS) {
                return new LoginAttempt(1, System.currentTimeMillis());
            }
            attempt.count++;
            return attempt;
        });
    }

    private static class LoginAttempt {
        int count;
        long firstAttemptAt;

        LoginAttempt(int count, long firstAttemptAt) {
            this.count = count;
            this.firstAttemptAt = firstAttemptAt;
        }
    }
}
