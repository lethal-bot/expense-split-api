package com.example.expense_split.service;

import com.example.expense_split.dto.CreateGroupRequest;
import com.example.expense_split.model.Group;
import com.example.expense_split.model.User;
import com.example.expense_split.repo.GroupRepository;
import com.example.expense_split.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Transactional
    public Group createGroup(CreateGroupRequest request, User currentUser) {
        // Fetch managed user from database to avoid detached entity exceptions
        User user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + currentUser.getUserId()));

        // Create and save Group
        Group group = new Group();
        group.setName(request.getName());
        group.setAdminId(user.getUserId());
        group.setIsActive("true");
        group.setMembers(new ArrayList<>(List.of(user)));
        group = groupRepository.save(group);

        // Update the owning side of the ManyToMany relationship
        if (user.getGroups() == null) {
            user.setGroups(new ArrayList<>());
        }
        user.getGroups().add(group);
        userRepository.save(user);

        return group;
    }

    @Transactional(readOnly = true)
    public List<Group> getMyGroups(User currentUser) {
        User user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + currentUser.getUserId()));

        List<Group> groups = user.getGroups();
        if (groups != null) {
            for (Group group : groups) {
                // Initialize the lazy-loaded members collection
                group.getMembers().size();
            }
        }
        return groups;
    }
}
