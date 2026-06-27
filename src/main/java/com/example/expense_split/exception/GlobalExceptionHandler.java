package com.example.expense_split.exception;

import com.example.expense_split.dto.ResponseTypeDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ResponseTypeDto<Object>> handleEmailAlreadyInUse(
            EmailAlreadyInUseException ex, WebRequest request) {
        
        ResponseTypeDto<Object> body = ResponseTypeDto.<Object>builder()
                .status("ERROR")
                .message(ex.getMessage())
                .data(null)
                .error("Conflict")
                .timestamp(LocalDateTime.now().toString())
                .build();

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseTypeDto<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        ResponseTypeDto<Object> body = ResponseTypeDto.<Object>builder()
                .status("ERROR")
                .message(ex.getMessage())
                .data(null)
                .error("Bad Request")
                .timestamp(LocalDateTime.now().toString())
                .build();

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
