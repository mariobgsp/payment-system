package com.java.project.msorder.exception.model;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ApiFault {

    private String status;
    private String code;
    private String type;
    private String message;
    private String detail;

}