package com.java.project.msorder.exception.definition;

import com.java.project.msorder.exception.model.ApiFault;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class CommonException extends Exception{

    private String status;
    private HttpStatus httpStatus;
    private String code;
    private String type;
    private String displayMessage;

    public CommonException(HttpStatus httpStatus, String code, String type, String displayMessage, String message) {
        super(message);
        this.status = httpStatus.is5xxServerError() ? "systemError" : "businessError";
        this.httpStatus = httpStatus;
        this.code = code;
        this.type = type;
        this.displayMessage = displayMessage;
    }

    public CommonException(Exception e) {
        super(e.getMessage());
        this.status = "systemError";
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.code = "99";
        this.type = e.getClass().getSimpleName();
        this.displayMessage = "Unknown Error: " + e.getClass().getSimpleName();
    }

}
