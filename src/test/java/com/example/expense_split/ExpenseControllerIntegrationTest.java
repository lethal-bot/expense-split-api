package com.example.expense_split;

import com.example.expense_split.dto.AddExpenseRequest;
import com.example.expense_split.model.Expense;
import com.example.expense_split.model.Group;
import com.example.expense_split.model.User;
import com.example.expense_split.repo.ExpenseRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ExpenseControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testAddExpenseEndpoint() throws Exception {
        // 1. Create and save users
        User creator = new User();
        creator.setName("Creator User");
        creator.setEmail("creator_exp@example.com");
        creator.setPassword("password");
        creator.setIsActive("true");
        creator.setGroups(new ArrayList<>());
        creator = userRepository.save(creator);

        User friend = new User();
        friend.setName("Friend User");
        friend.setEmail("friend_exp@example.com");
        friend.setPassword("password");
        friend.setIsActive("true");
        friend.setGroups(new ArrayList<>());
        friend = userRepository.save(friend);

        // 2. Create Group
        Group group = new Group();
        group.setName("Trip Group");
        group.setAdminId(creator.getUserId());
        group.setIsActive("true");
        group.setMembers(new ArrayList<>(List.of(creator, friend)));
        group = groupRepository.save(group);

        creator.getGroups().add(group);
        friend.getGroups().add(group);
        userRepository.save(creator);
        userRepository.save(friend);

        // 3. Perform request
        AddExpenseRequest request = new AddExpenseRequest(
                group.getGroupId(),
                120L,
                "Taxi ride",
                List.of(friend.getUserId())
        );

        String responseContent = mockMvc.perform(post("/api/v1/expense/add-expense")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 4. Asserts
        Expense createdExpense = objectMapper.readValue(responseContent, Expense.class);
        assertNotNull(createdExpense);
        assertNotNull(createdExpense.getExpenseId());
        assertEquals(120L, createdExpense.getTotalExpenseAmount());
        assertEquals("Taxi ride", createdExpense.getDescriptionOfExpense());
        assertEquals(1, createdExpense.getSplits().size());
        assertEquals(friend.getUserId(), createdExpense.getSplits().get(0).getFriend().getUserId());
        assertEquals(creator.getUserId(), createdExpense.getSplits().get(0).getUser().getUserId());
        assertTrue(createdExpense.getSplits().get(0).getIsActive());
    }
}
