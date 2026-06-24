package com.example.expense_split.service;

import com.example.expense_split.dto.AddExpenseRequest;
import com.example.expense_split.model.Expense;
import com.example.expense_split.model.ExpenseSplit;
import com.example.expense_split.model.Group;
import com.example.expense_split.model.User;
import com.example.expense_split.repo.ExpenseRepository;
import com.example.expense_split.repo.ExpenseSplitRepository;
import com.example.expense_split.repo.GroupRepository;
import com.example.expense_split.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Transactional
    public Expense addExpense(AddExpenseRequest request, User currentUser) {
        // 1. Fetch creator user
        User creator = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + currentUser.getUserId()));

        // 2. Fetch Group
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + request.getGroupId()));

        // 3. Create and Save Expense
        Expense expense = new Expense();
        expense.setName(request.getName());
        expense.setCreatedBy(creator);
        expense.setTotalExpenseAmount(request.getTotalExpenseAmount());
        expense.setDescriptionOfExpense(request.getDescriptionOfExpense());
        expense.setGroup(group);
        expense.setSplits(new ArrayList<>());
        expense = expenseRepository.save(expense);

        // 4. Create and Save splits
        List<ExpenseSplit> splits = new ArrayList<>();
        if (request.getFriendIds() != null) {
            for (Long friendId : request.getFriendIds()) {
                User friend = userRepository.findById(friendId)
                        .orElseThrow(() -> new IllegalArgumentException("Friend user not found with ID: " + friendId));

                ExpenseSplit split = new ExpenseSplit();
                split.setExpense(expense);
                split.setUser(creator);
                split.setFriend(friend);
                split.setFriendApproveFlag("false");
                split.setAdminApproveFlag("false");
                split.setIsActive(true);
                splits.add(expenseSplitRepository.save(split));
            }
        }
        expense.setSplits(splits);

        return expense;
    }
}
