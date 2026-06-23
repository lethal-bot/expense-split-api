package com.example.expense_split.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class User {

    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }
}
