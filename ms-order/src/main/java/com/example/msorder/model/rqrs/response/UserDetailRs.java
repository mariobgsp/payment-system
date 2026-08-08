package com.example.msorder.model.rqrs.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetailRs {

    private int id;
    private String userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private boolean specialProduct;
    private boolean recurring;
    private String token;
}
