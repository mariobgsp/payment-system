package com.example.msorder.model.rqrs.request.order;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserDetail {

    private String username;
    private String password;
}
