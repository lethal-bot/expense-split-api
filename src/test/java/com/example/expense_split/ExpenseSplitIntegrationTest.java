package com.example.expense_split;

import com.example.expense_split.model.Expense;
import com.example.expense_split.model.ExpenseSplit;
import com.example.expense_split.model.Group;
import com.example.expense_split.model.User;
import com.example.expense_split.repo.ExpenseRepository;
import com.example.expense_split.repo.ExpenseSplitRepository;
import com.example.expense_split.repo.GroupRepository;
import com.example.expense_split.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ExpenseSplitIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseSplitRepository expenseSplitRepository;

    @Test
    void testCreateExpenseAndSplits() {
        // 1. Create creator and friend users
        User creator = new User();
        creator.setName("Creator User");
        creator.setEmail("creator@example.com");
        creator.setPassword("password");
        creator.setIsActive("true");
        creator.setGroups(new ArrayList<>());
        creator = userRepository.save(creator);

        User friend = new User();
        friend.setName("Friend User");
        friend.setEmail("friend@example.com");
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

        // Update user groups relationship
        creator.getGroups().add(group);
        friend.getGroups().add(group);
        userRepository.save(creator);
        userRepository.save(friend);

        // 3. Create Expense
        Expense expense = new Expense();
        expense.setCreatedBy(creator);
        expense.setTotalExpenseAmount(100L);
        expense.setDescriptionOfExpense("Dinner split");
        expense.setGroup(group);
        expense = expenseRepository.save(expense);

        // 4. Create ExpenseSplit
        ExpenseSplit split = new ExpenseSplit();
        split.setExpense(expense);
        split.setUser(creator);
        split.setFriend(friend);
        split.setFriendApproveFlag("false");
        split.setAdminApproveFlag("false");
        split.setIsActive("true");
        split = expenseSplitRepository.save(split);

        // 5. Verify the details
        assertNotNull(expense.getExpenseId());
        assertNotNull(split.getTransactionId());

        Expense savedExpense = expenseRepository.findById(expense.getExpenseId()).orElse(null);
        assertNotNull(savedExpense);
        assertEquals(100L, savedExpense.getTotalExpenseAmount());
        assertEquals("Dinner split", savedExpense.getDescriptionOfExpense());
        assertEquals(creator.getUserId(), savedExpense.getCreatedBy().getUserId());
        assertEquals(group.getGroupId(), savedExpense.getGroup().getGroupId());

        ExpenseSplit savedSplit = expenseSplitRepository.findById(split.getTransactionId()).orElse(null);
        assertNotNull(savedSplit);
        assertEquals(expense.getExpenseId(), savedSplit.getExpense().getExpenseId());
        assertEquals(creator.getUserId(), savedSplit.getUser().getUserId());
        assertEquals(friend.getUserId(), savedSplit.getFriend().getUserId());
        assertEquals("false", savedSplit.getFriendApproveFlag());
        assertEquals("false", savedSplit.getAdminApproveFlag());
        assertEquals(true, savedSplit.getIsActive());
    }
}
