package com.example.expense_split.controller;

import com.example.expense_split.dto.CreateGroupRequest;
import com.example.expense_split.model.Group;
import com.example.expense_split.model.User;
import com.example.expense_split.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.expense_split.dto.ResponseTypeDto;
import java.util.List;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/create-group")
    public ResponseEntity<ResponseTypeDto<Group>> createGroup(
            @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal User currentUser) {
        Group createdGroup = groupService.createGroup(request, currentUser);
        return ResponseEntity.ok(ResponseTypeDto.success("Group created successfully", createdGroup));
    }

    @GetMapping("/my-groups")
    public ResponseEntity<ResponseTypeDto<List<Group>>> getMyGroups(
            @AuthenticationPrincipal User currentUser
    ) {
        List<Group> groups = groupService.getMyGroups(currentUser);
        return ResponseEntity.ok(ResponseTypeDto.success("Groups retrieved successfully", groups));
    }
}
