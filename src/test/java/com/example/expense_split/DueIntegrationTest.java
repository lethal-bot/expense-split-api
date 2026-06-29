package com.example.expense_split;

import com.example.expense_split.dto.AddExpenseRequest;
import com.example.expense_split.dto.RespondRequestDto;
import com.example.expense_split.model.*;
import com.example.expense_split.repo.*;
import com.example.expense_split.service.ExpenseService;
import com.example.expense_split.service.UserRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DueIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRequestRepository userRequestRepository;

    @Autowired
    private DueRepository dueRepository;

    @Autowired
    private UserRequestService userRequestService;

    @Autowired
    private ExpenseService expenseService;

    @Test
    void testDuesInitializedOnGroupAccept() {
        // 1. Create admin user
        User admin = new User();
        admin.setName("Group Admin");
        admin.setEmail("admin_due@example.com");
        admin.setPassword("password");
        admin.setIsActive("true");
        admin.setGroups(new ArrayList<>());
        admin = userRepository.save(admin);

        // 2. Create group
        Group group = new Group();
        group.setName("Due Trip");
        group.setAdminId(admin.getUserId());
        group.setIsActive("true");
        group.setMembers(new ArrayList<>(List.of(admin)));
        group = groupRepository.save(group);

        admin.getGroups().add(group);
        userRepository.save(admin);

        // 3. Create target user who wants to join
        User newMember = new User();
        newMember.setName("New Member");
        newMember.setEmail("member_due@example.com");
        newMember.setPassword("password");
        newMember.setIsActive("true");
        newMember.setGroups(new ArrayList<>());
        newMember = userRepository.save(newMember);

        // 4. Create pending request
        UserRequest request = new UserRequest();
        request.setUser(newMember);
        request.setGroup(group);
        request.setStatus(RequestStatus.PENDING);
        userRequestRepository.save(request);

        // 5. Accept request (this should trigger dues initialization)
        RespondRequestDto respondDto = new RespondRequestDto(group.getGroupId(), RequestStatus.ACCEPTED);
        userRequestService.respondToRequest(respondDto, newMember);

        // 6. Verify Due record exists between admin and newMember
        User get;
        User give;
        if (admin.getUserId() < newMember.getUserId()) {
            get = admin;
            give = newMember;
        } else {
            get = newMember;
            give = admin;
        }

        Due due = dueRepository.findByUserWhichWillGetAndUserWhichWillGiveAndGroup(get, give, group).orElse(null);
        assertNotNull(due);
        assertEquals(0.0, due.getAmount());
        assertTrue(due.getActive());
        assertEquals(get.getUserId(), due.getUserWhichWillGet().getUserId());
        assertEquals(give.getUserId(), due.getUserWhichWillGive().getUserId());
    }

    @Test
    void testDuesUpdatedOnExpenseSplit() {
        // 1. Create two users
        User userA = new User();
        userA.setName("User A");
        userA.setEmail("usera@example.com");
        userA.setPassword("password");
        userA.setIsActive("true");
        userA.setGroups(new ArrayList<>());
        userA = userRepository.save(userA);

        User userB = new User();
        userB.setName("User B");
        userB.setEmail("userb@example.com");
        userB.setPassword("password");
        userB.setIsActive("true");
        userB.setGroups(new ArrayList<>());
        userB = userRepository.save(userB);

        // Determine who has lower ID (get) and higher ID (give)
        User lowerUser = userA.getUserId() < userB.getUserId() ? userA : userB;
        User higherUser = userA.getUserId() < userB.getUserId() ? userB : userA;

        // 2. Create group with both members
        Group group = new Group();
        group.setName("Shared Expenses");
        group.setAdminId(lowerUser.getUserId());
        group.setIsActive("true");
        group.setMembers(new ArrayList<>(List.of(lowerUser, higherUser)));
        group = groupRepository.save(group);

        lowerUser.getGroups().add(group);
        higherUser.getGroups().add(group);
        userRepository.save(lowerUser);
        userRepository.save(higherUser);

        // 3. Initialize due record manually (since we bypassed respondToRequest)
        Due due = Due.builder()
                .userWhichWillGet(lowerUser)
                .userWhichWillGive(higherUser)
                .group(group)
                .amount(0.0)
                .active(true)
                .build();
        due = dueRepository.save(due);

        // Case 1: Creator of expense is lowerUser (get). Friend is higherUser (give).
        // Total expense = 100. Since split is between 1 friend, contribution is 100.
        // higherUser (give) owes lowerUser (get) 100. Amount increases.
        AddExpenseRequest request1 = new AddExpenseRequest(
                group.getGroupId(),
                "Expense 1",
                100L,
                "Lower User Pays",
                List.of(higherUser.getUserId())
        );
        expenseService.addExpense(request1, lowerUser);

        Due updatedDue1 = dueRepository.findById(due.getDueId()).orElse(null);
        assertNotNull(updatedDue1);
        assertEquals(100.0, updatedDue1.getAmount());

        // Case 2: Creator of expense is higherUser (give). Friend is lowerUser (get).
        // Total expense = 40. Split contribution is 40.
        // lowerUser (get) owes higherUser (give) 40. Amount decreases.
        AddExpenseRequest request2 = new AddExpenseRequest(
                group.getGroupId(),
                "Expense 2",
                40L,
                "Higher User Pays",
                List.of(lowerUser.getUserId())
        );
        expenseService.addExpense(request2, higherUser);

        Due updatedDue2 = dueRepository.findById(due.getDueId()).orElse(null);
        assertNotNull(updatedDue2);
        assertEquals(60.0, updatedDue2.getAmount()); // 100.0 - 40.0 = 60.0
    }
}
