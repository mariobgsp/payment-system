package com.example.msorder.model.rqrs.request.auth;

import lombok.Data;

@Data
public class LoginRq {
    private String username;
    private String password;
}
