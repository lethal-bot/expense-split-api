package com.example.expense_split.controller;

import com.example.expense_split.dto.ResponseTypeDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class User {

    @GetMapping("/hello")
    public ResponseEntity<ResponseTypeDto<String>> hello(){
        return ResponseEntity.ok(ResponseTypeDto.success("Success", "hello"));
    }
}
