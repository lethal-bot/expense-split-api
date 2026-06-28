package com.example.expense_split;

import com.example.expense_split.model.Group;
import com.example.expense_split.model.RequestStatus;
import com.example.expense_split.model.User;
import com.example.expense_split.model.UserRequest;
import com.example.expense_split.model.UserRequestId;
import com.example.expense_split.repo.GroupRepository;
import com.example.expense_split.repo.UserRepository;
import com.example.expense_split.repo.UserRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserRequestIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRequestRepository userRequestRepository;

    @Test
    void testCreateQueryAndUpdateUserRequest() {
        // 1. Create a User
        User user = new User();
        user.setName("Requesting User");
        user.setEmail("requester@example.com");
        user.setPassword("password");
        user.setIsActive("true");
        user.setGroups(new ArrayList<>());
        user = userRepository.save(user);

        // 2. Create a Group
        Group group = new Group();
        group.setName("Project Group");
        group.setAdminId(user.getUserId());
        group.setIsActive("true");
        group.setMembers(new ArrayList<>());
        group = groupRepository.save(group);

        // 3. Create a UserRequest
        UserRequest request = new UserRequest();
        request.setUser(user);
        request.setGroup(group);
        request.setStatus(RequestStatus.PENDING);
        request = userRequestRepository.save(request);

        assertNotNull(request.getCreatedDate());
        assertNotNull(request.getModifiedDate());

        // 4. Verify composite ID structure is generated and persisted correctly
        UserRequestId id = new UserRequestId(user.getUserId(), group.getGroupId());
        UserRequest savedRequest = userRequestRepository.findById(id).orElse(null);

        assertNotNull(savedRequest);
        assertEquals(user.getUserId(), savedRequest.getUser().getUserId());
        assertEquals(group.getGroupId(), savedRequest.getGroup().getGroupId());
        assertEquals(RequestStatus.PENDING, savedRequest.getStatus());
        assertNotNull(savedRequest.getCreatedDate());
        assertNotNull(savedRequest.getModifiedDate());

        // 5. Update Status
        savedRequest.setStatus(RequestStatus.ACCEPTED);
        userRequestRepository.save(savedRequest);

        UserRequest updatedRequest = userRequestRepository.findById(id).orElse(null);
        assertNotNull(updatedRequest);
        assertEquals(RequestStatus.ACCEPTED, updatedRequest.getStatus());
        assertNotNull(updatedRequest.getCreatedDate());
        assertNotNull(updatedRequest.getModifiedDate());
    }
}
