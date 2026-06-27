package com.example.expense_split.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ResponseTypeDto<T> {

    private String status;
    private String message;
    private T data;
    private String error;
    private String timestamp;

    public static <T> ResponseTypeDto<T> success(String message, T data) {
        return ResponseTypeDto.<T>builder()
                .status("SUCCESS")
                .message(message)
                .data(data)
                .error(null)
                .timestamp(java.time.LocalDateTime.now().toString())
                .build();
    }

    public static <T> ResponseTypeDto<T> error(String message, T data) {
        return ResponseTypeDto.<T>builder()
                .status("ERROR")
                .message(message)
                .data(data)
                .error(null)
                .timestamp(java.time.LocalDateTime.now().toString())
                .build();
    }

}
