package com.example.expense_split.service;

import com.example.expense_split.dto.RespondRequestDto;
import com.example.expense_split.dto.SendRequestDto;
import com.example.expense_split.model.Group;
import com.example.expense_split.model.RequestStatus;
import com.example.expense_split.model.User;
import com.example.expense_split.model.UserRequest;
import com.example.expense_split.model.UserRequestId;
import com.example.expense_split.repo.GroupRepository;
import com.example.expense_split.repo.UserRepository;
import com.example.expense_split.repo.UserRequestRepository;
import com.example.expense_split.repo.DueRepository;
import com.example.expense_split.model.Due;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRequestService {

    private final UserRequestRepository userRequestRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final DueRepository dueRepository;

    @Transactional
    public UserRequest sendJoiningRequest(SendRequestDto requestDto) {
        User targetUser = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + requestDto.getEmail()));

        Group group = groupRepository.findById(requestDto.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + requestDto.getGroupId()));

        UserRequestId requestId = new UserRequestId(targetUser.getUserId(), group.getGroupId());

        return userRequestRepository.findById(requestId)
                .map(existingRequest -> {
                    if (existingRequest.getStatus() == RequestStatus.REJECTED) {
                        existingRequest.setStatus(RequestStatus.PENDING);
                        return userRequestRepository.save(existingRequest);
                    }
                    throw new IllegalArgumentException(
                            "Request already exists with status: " + existingRequest.getStatus());
                })
                .orElseGet(() -> {
                    UserRequest newRequest = new UserRequest();
                    newRequest.setUser(targetUser);
                    newRequest.setGroup(group);
                    newRequest.setStatus(RequestStatus.PENDING);
                    return userRequestRepository.save(newRequest);
                });
    }

    @Transactional
    public UserRequest respondToRequest(RespondRequestDto responseDto, User currentUser) {
        // this api is for accepting the request from a group
        // whats happening in this api
        // checking that requet send is accepted or rejected
        // userRequest fetching to check if it is already present or not <- db call 1
        // saving the updated user request <- db call 2
        // fetching the user to add the group <- db call 3
        // fetching the group to add the user <- db call 4
        // fetching the members to add in the due table <- db call 5
        // adding the bulk combinations of users and group in due table <- db call 6

        if (responseDto.getStatus() != RequestStatus.ACCEPTED && responseDto.getStatus() != RequestStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid response status. Must be ACCEPTED or REJECTED.");
        }

        UserRequestId requestId = new UserRequestId(currentUser.getUserId(), responseDto.getGroupId());
        UserRequest userRequest = userRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found for this group."));

        if (userRequest.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Request is not in PENDING status. Current status: " + userRequest.getStatus());
        }

        userRequest.setStatus(responseDto.getStatus());
        UserRequest savedRequest = userRequestRepository.save(userRequest);

        if (responseDto.getStatus() == RequestStatus.ACCEPTED) {
            User user = userRepository.findById(currentUser.getUserId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("User not found with ID: " + currentUser.getUserId()));
            Group group = groupRepository.findById(responseDto.getGroupId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Group not found with ID: " + responseDto.getGroupId()));

            if (user.getGroups() == null) {
                user.setGroups(new ArrayList<>());
            }
            if (!user.getGroups().contains(group)) {
                user.getGroups().add(group);
                userRepository.save(user);
            }

            if (group.getMembers() == null) {
                group.setMembers(new ArrayList<>());
            }
            List<Due> dueList = new ArrayList<>();
            if (!group.getMembers().contains(user)) {
                for (User member : group.getMembers()) {
                    if (member.getUserId().equals(user.getUserId())) {
                        continue;
                    }
                    User get;
                    User give;
                    if (user.getUserId() < member.getUserId()) {
                        get = user;
                        give = member;
                    } else {
                        get = member;
                        give = user;
                    }

                    Due due = Due.builder()
                            .userWhichWillGet(get)
                            .userWhichWillGive(give)
                            .group(group)
                            .amount(0.0)
                            .active(true)
                            .build();

                    dueList.add(due);
                }
                if (!dueList.isEmpty()) {
                    dueRepository.saveAll(dueList);
                }
                group.getMembers().add(user);
                groupRepository.save(group);
            }
        }

        return savedRequest;
    }

    public List<UserRequest> getPendingRequests(User currentUser) {
        return userRequestRepository.findByUserUserIdAndStatus(currentUser.getUserId(), RequestStatus.PENDING);
    }
}
