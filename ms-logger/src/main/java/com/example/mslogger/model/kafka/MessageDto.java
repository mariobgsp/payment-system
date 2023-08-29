package com.example.mslogger.model.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageDto {

    @JsonProperty("message")
    @SerializedName("message")
    private String message;

    @JsonProperty("messageType")
    @SerializedName("messageType")
    private String messageType;
}
