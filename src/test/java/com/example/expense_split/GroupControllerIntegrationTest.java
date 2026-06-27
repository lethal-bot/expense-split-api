package com.example.expense_split;

import com.example.expense_split.dto.CreateGroupRequest;
import com.example.expense_split.model.Group;
import com.example.expense_split.model.User;
import com.example.expense_split.repo.GroupRepository;
import com.example.expense_split.repo.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class GroupControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testCreateGroupEndpoint() throws Exception {
        // 1. Create and save a user
        User user = new User();
        user.setName("Test Admin");
        user.setEmail("admin@example.com");
        user.setPassword("password");
        user.setIsActive("true");
        user.setGroups(new ArrayList<>());
        user = userRepository.save(user);

        // 2. Perform the post request
        CreateGroupRequest request = new CreateGroupRequest("Family Budget");

        String responseContent = mockMvc.perform(post("/api/v1/group/create-group")
                        .with(user(user)) // Pass the mock user detail principal
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 3. Deserialize response and assert fields
        java.util.Map<?, ?> responseMap = objectMapper.readValue(responseContent, java.util.Map.class);
        assertNotNull(responseMap);
        assertEquals("SUCCESS", responseMap.get("status"));
        Group createdGroup = objectMapper.convertValue(responseMap.get("data"), Group.class);
        assertNotNull(createdGroup);
        assertNotNull(createdGroup.getGroupId());
        assertEquals("Family Budget", createdGroup.getName());
        assertEquals(user.getUserId(), createdGroup.getAdminId());
        assertEquals("true", createdGroup.getIsActive());

        // Verify relationship mapping in DB
        Group savedGroup = groupRepository.findById(createdGroup.getGroupId()).orElse(null);
        assertNotNull(savedGroup);
        assertEquals(1, savedGroup.getMembers().size());
        assertEquals(user.getUserId(), savedGroup.getMembers().get(0).getUserId());

        User savedUser = userRepository.findById(user.getUserId()).orElse(null);
        assertNotNull(savedUser);
        assertEquals(1, savedUser.getGroups().size());
        assertEquals(savedGroup.getGroupId(), savedUser.getGroups().get(0).getGroupId());
    }

    @Test
    void testGetMyGroupsEndpoint() throws Exception {
        // 1. Create and save user and group
        User user = new User();
        user.setName("Member User");
        user.setEmail("member@example.com");
        user.setPassword("password");
        user.setIsActive("true");
        user.setGroups(new ArrayList<>());
        user = userRepository.save(user);

        Group group = new Group();
        group.setName("Shared Budget");
        group.setAdminId(user.getUserId());
        group.setIsActive("true");
        group.setMembers(new ArrayList<>(java.util.List.of(user)));
        group = groupRepository.save(group);

        user.getGroups().add(group);
        userRepository.save(user);

        // 2. Perform GET my-groups request
        String responseContent = mockMvc.perform(get("/api/v1/group/my-groups")
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 3. Deserialize list of groups
        java.util.Map<?, ?> responseMap = objectMapper.readValue(responseContent, java.util.Map.class);
        assertNotNull(responseMap);
        assertEquals("SUCCESS", responseMap.get("status"));
        Group[] groups = objectMapper.convertValue(responseMap.get("data"), Group[].class);
        assertNotNull(groups);
        assertEquals(1, groups.length);
        assertEquals("Shared Budget", groups[0].getName());
        assertEquals(1, groups[0].getMemberCount());
        assertNull(groups[0].getMembers());

        // Assert that expenses are NOT serialized (will be null due to @JsonIgnore)
        assertNull(groups[0].getExpenses());
    }
}
