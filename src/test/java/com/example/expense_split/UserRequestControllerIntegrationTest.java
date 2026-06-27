package com.example.expense_split;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class UserRequestControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRequestRepository userRequestRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User requester;
    private User targetUser;
    private Group group;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Create and save a requester user (admin of group)
        requester = new User();
        requester.setName("Requester Admin");
        requester.setEmail("requester_admin@example.com");
        requester.setPassword("password");
        requester.setIsActive("true");
        requester.setGroups(new ArrayList<>());
        requester = userRepository.save(requester);

        // Create and save a target user to be invited
        targetUser = new User();
        targetUser.setName("Target User");
        targetUser.setEmail("target_user@example.com");
        targetUser.setPassword("password");
        targetUser.setIsActive("true");
        targetUser.setGroups(new ArrayList<>());
        targetUser = userRepository.save(targetUser);

        // Create Group
        group = new Group();
        group.setName("Shared Group");
        group.setAdminId(requester.getUserId());
        group.setIsActive("true");
        group.setMembers(new ArrayList<>(java.util.List.of(requester)));
        group = groupRepository.save(group);

        requester.getGroups().add(group);
        userRepository.save(requester);
    }

    @Test
    void testSendRequestNew() throws Exception {
        SendRequestDto requestDto = new SendRequestDto(targetUser.getEmail(), group.getGroupId());

        String responseContent = mockMvc.perform(post("/api/v1/group-request/send-request")
                        .with(user(requester))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        java.util.Map<?, ?> responseMap = objectMapper.readValue(responseContent, java.util.Map.class);
        assertNotNull(responseMap);
        assertEquals("SUCCESS", responseMap.get("status"));
        java.util.Map<?, ?> dataMap = (java.util.Map<?, ?>) responseMap.get("data");
        assertEquals("pending", dataMap.get("message"));

        // Verify database state
        UserRequestId id = new UserRequestId(targetUser.getUserId(), group.getGroupId());
        UserRequest saved = userRequestRepository.findById(id).orElse(null);
        assertNotNull(saved);
        assertEquals(RequestStatus.PENDING, saved.getStatus());
    }

    @Test
    void testSendRequestRejectedToPending() throws Exception {
        // Pre-insert a rejected request
        UserRequest existing = new UserRequest();
        existing.setUser(targetUser);
        existing.setGroup(group);
        existing.setStatus(RequestStatus.REJECTED);
        userRequestRepository.save(existing);

        SendRequestDto requestDto = new SendRequestDto(targetUser.getEmail(), group.getGroupId());

        String responseContent = mockMvc.perform(post("/api/v1/group-request/send-request")
                        .with(user(requester))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        java.util.Map<?, ?> responseMap = objectMapper.readValue(responseContent, java.util.Map.class);
        assertNotNull(responseMap);
        assertEquals("SUCCESS", responseMap.get("status"));
        java.util.Map<?, ?> dataMap = (java.util.Map<?, ?>) responseMap.get("data");
        assertEquals("pending", dataMap.get("message"));

        // Verify database state
        UserRequestId id = new UserRequestId(targetUser.getUserId(), group.getGroupId());
        UserRequest saved = userRequestRepository.findById(id).orElse(null);
        assertNotNull(saved);
        assertEquals(RequestStatus.PENDING, saved.getStatus());
    }

    @Test
    void testSendRequestDuplicatePendingThrowsException() throws Exception {
        // Pre-insert a pending request
        UserRequest existing = new UserRequest();
        existing.setUser(targetUser);
        existing.setGroup(group);
        existing.setStatus(RequestStatus.PENDING);
        userRequestRepository.save(existing);

        SendRequestDto requestDto = new SendRequestDto(targetUser.getEmail(), group.getGroupId());

        mockMvc.perform(post("/api/v1/group-request/send-request")
                        .with(user(requester))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()); // should return 400 bad request due to IllegalArgumentException
    }

    @Test
    void testSendRequestNonExistentEmailThrowsException() throws Exception {
        SendRequestDto requestDto = new SendRequestDto("nonexistent@example.com", group.getGroupId());

        mockMvc.perform(post("/api/v1/group-request/send-request")
                        .with(user(requester))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()); // should return 400 bad request due to User not found
    }

    @Test
    void testRespondAcceptPending() throws Exception {
        // Pre-insert a pending request
        UserRequest existing = new UserRequest();
        existing.setUser(targetUser);
        existing.setGroup(group);
        existing.setStatus(RequestStatus.PENDING);
        userRequestRepository.save(existing);

        RespondRequestDto respondDto = new RespondRequestDto(group.getGroupId(), RequestStatus.ACCEPTED);

        String responseContent = mockMvc.perform(post("/api/v1/group-request/respond")
                        .with(user(targetUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respondDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        java.util.Map<?, ?> responseMap = objectMapper.readValue(responseContent, java.util.Map.class);
        assertNotNull(responseMap);
        assertEquals("SUCCESS", responseMap.get("status"));
        java.util.Map<?, ?> dataMap = (java.util.Map<?, ?>) responseMap.get("data");
        assertEquals("accepted", dataMap.get("message"));

        // Verify relationship and database state
        UserRequestId id = new UserRequestId(targetUser.getUserId(), group.getGroupId());
        UserRequest saved = userRequestRepository.findById(id).orElse(null);
        assertNotNull(saved);
        assertEquals(RequestStatus.ACCEPTED, saved.getStatus());

        User savedUser = userRepository.findById(targetUser.getUserId()).orElse(null);
        assertNotNull(savedUser);
        assertTrue(savedUser.getGroups().stream().anyMatch(g -> g.getGroupId().equals(group.getGroupId())));

        Group savedGroup = groupRepository.findById(group.getGroupId()).orElse(null);
        assertNotNull(savedGroup);
        assertTrue(savedGroup.getMembers().stream().anyMatch(u -> u.getUserId().equals(targetUser.getUserId())));
    }

    @Test
    void testRespondRejectPending() throws Exception {
        // Pre-insert a pending request
        UserRequest existing = new UserRequest();
        existing.setUser(targetUser);
        existing.setGroup(group);
        existing.setStatus(RequestStatus.PENDING);
        userRequestRepository.save(existing);

        RespondRequestDto respondDto = new RespondRequestDto(group.getGroupId(), RequestStatus.REJECTED);

        String responseContent = mockMvc.perform(post("/api/v1/group-request/respond")
                        .with(user(targetUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respondDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        java.util.Map<?, ?> responseMap = objectMapper.readValue(responseContent, java.util.Map.class);
        assertNotNull(responseMap);
        assertEquals("SUCCESS", responseMap.get("status"));
        java.util.Map<?, ?> dataMap = (java.util.Map<?, ?>) responseMap.get("data");
        assertEquals("rejected", dataMap.get("message"));

        // Verify relationship in DB (should NOT be added to groups or members)
        UserRequestId id = new UserRequestId(targetUser.getUserId(), group.getGroupId());
        UserRequest saved = userRequestRepository.findById(id).orElse(null);
        assertNotNull(saved);
        assertEquals(RequestStatus.REJECTED, saved.getStatus());

        User savedUser = userRepository.findById(targetUser.getUserId()).orElse(null);
        assertNotNull(savedUser);
        assertFalse(savedUser.getGroups().stream().anyMatch(g -> g.getGroupId().equals(group.getGroupId())));
    }

    @Test
    void testRespondAlreadyProcessedThrows() throws Exception {
        // Pre-insert an already accepted request
        UserRequest existing = new UserRequest();
        existing.setUser(targetUser);
        existing.setGroup(group);
        existing.setStatus(RequestStatus.ACCEPTED);
        userRequestRepository.save(existing);

        RespondRequestDto respondDto = new RespondRequestDto(group.getGroupId(), RequestStatus.REJECTED);

        mockMvc.perform(post("/api/v1/group-request/respond")
                        .with(user(targetUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respondDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRespondNonExistentThrows() throws Exception {
        RespondRequestDto respondDto = new RespondRequestDto(group.getGroupId(), RequestStatus.ACCEPTED);

        mockMvc.perform(post("/api/v1/group-request/respond")
                        .with(user(targetUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respondDto)))
                .andExpect(status().isBadRequest());
    }
}
