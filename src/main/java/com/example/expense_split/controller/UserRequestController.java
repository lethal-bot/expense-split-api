package com.example.expense_split.controller;

import com.example.expense_split.dto.RespondRequestDto;
import com.example.expense_split.dto.ResponseTypeDto;
import com.example.expense_split.dto.SendRequestDto;
import com.example.expense_split.model.User;
import com.example.expense_split.model.UserRequest;
import com.example.expense_split.service.UserRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/group-request")
@RequiredArgsConstructor
public class UserRequestController {

    private final UserRequestService userRequestService;

    @PostMapping("/send-request")
    public ResponseEntity<ResponseTypeDto<Map<String, String>>> sendRequest(@RequestBody SendRequestDto requestDto) {
        userRequestService.sendJoiningRequest(requestDto);
        return ResponseEntity.ok(ResponseTypeDto.success("Request sent successfully", Map.of("message", "pending")));
    }

    @PostMapping("/respond")
    public ResponseEntity<ResponseTypeDto<Map<String, String>>> respondRequest(
            @RequestBody RespondRequestDto responseDto,
            @AuthenticationPrincipal User currentUser) {
        userRequestService.respondToRequest(responseDto, currentUser);
        String actionStatus = responseDto.getStatus().toString().toLowerCase();
        return ResponseEntity.ok(
                ResponseTypeDto.success("Request " + actionStatus + " successfully", Map.of("message", actionStatus)));
    }

    @GetMapping("/pending")
    public ResponseEntity<ResponseTypeDto<List<UserRequest>>> getPendingRequests(
            @AuthenticationPrincipal User currentUser) {
        List<UserRequest> pendingRequests = userRequestService.getPendingRequests(currentUser);
        return ResponseEntity.ok(ResponseTypeDto.success("Pending requests retrieved successfully", pendingRequests));
    }
}
