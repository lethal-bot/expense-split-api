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
import com.example.expense_split.utililty.HelperMethods;
import com.example.expense_split.repo.DueRepository;
import com.example.expense_split.model.Due;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final DueRepository dueRepository;

    @Transactional
    public Expense addExpense(AddExpenseRequest request, User currentUser) {

        // 2. Fetch Group
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + request.getGroupId()));

        // fetch people who are not in the group

        List<Long> usersInGroup = group.getMembers().stream().map(User::getUserId).collect(Collectors.toList());

        List<Long> peopleNotPresentInGroup = request.getFriendIds().stream()
                .filter(member -> !usersInGroup.contains(member))
                .collect(Collectors.toList());

        System.out.println(peopleNotPresentInGroup);

        if (!peopleNotPresentInGroup.isEmpty()) {
            throw new IllegalArgumentException("Some friends are not part of the group or groupId is incorrect");
        }

        // 3. Create and Save Expense
        Expense expense = new Expense();

        expense.setName(request.getName());
        expense.setCreatedBy(currentUser);
        expense.setTotalExpenseAmount(request.getTotalExpenseAmount());
        expense.setDescriptionOfExpense(request.getDescriptionOfExpense());
        expense.setGroup(group);
        expense.setSplits(new ArrayList<>());
        expense = expenseRepository.save(expense);

        // 4. Create and Save splits
        List<ExpenseSplit> splits = new ArrayList<>();
        if (request.getFriendIds() != null && !request.getFriendIds().isEmpty()) {
            int totalPeople = request.getFriendIds().size();
            double splitContribution = HelperMethods.splitAmount(request.getTotalExpenseAmount(), totalPeople);

            for (Long friendId : request.getFriendIds()) {
                ExpenseSplit split = new ExpenseSplit();
                User friendProxy = userRepository.getReferenceById(friendId);
                split.setExpense(expense);
                split.setUser(currentUser);
                split.setFriend(friendProxy);
                split.setFriendApproveFlag("false");
                split.setAdminApproveFlag("false");
                split.setIsActive(true);
                split.setExpenseContribution(splitContribution);
                splits.add(split);
            }
        }
        expenseSplitRepository.saveAll(splits);
        expense.setSplits(splits);

        // Update dues balances
        if (request.getFriendIds() != null && !request.getFriendIds().isEmpty()) {
            double splitContribution = HelperMethods.splitAmount(request.getTotalExpenseAmount(),
                    request.getFriendIds().size());

            User creator = userRepository.getReferenceById(currentUser.getUserId());

            // 1. Batch fetch all existing dues involving the creator in this group
            List<Due> existingDues = dueRepository.findByGroupAndUserInvolved(group, creator);

            // 2. Build map of existing dues for O(1) in-memory lookup
            Map<String, Due> dueMap = existingDues.stream().collect(Collectors.toMap(
                    due -> due.getUserWhichWillGet().getUserId() + "-" + due.getUserWhichWillGive().getUserId(),
                    due -> due
            ));

            List<Due> dueList = new ArrayList<>();
            for (Long friendId : request.getFriendIds()) {
                User friend = userRepository.getReferenceById(friendId);

                User get;
                User give;
                if (creator.getUserId().equals(friend.getUserId())) {
                    continue;
                } else if (creator.getUserId() < friend.getUserId()) {
                    get = creator;
                    give = friend;
                } else {
                    get = friend;
                    give = creator;
                }

                String key = get.getUserId() + "-" + give.getUserId();
                Due due = dueMap.get(key);
                if (due == null) {
                    due = Due.builder()
                            .userWhichWillGet(get)
                            .userWhichWillGive(give)
                            .group(group)
                            .amount(0.0)
                            .active(true)
                            .build();
                    due = dueRepository.save(due);
                    dueMap.put(key, due);
                }

                if (creator.getUserId().equals(get.getUserId())) {
                    due.setAmount(due.getAmount() + splitContribution);
                } else {
                    due.setAmount(due.getAmount() - splitContribution);
                }
                dueList.add(due);
            }
            dueRepository.saveAll(dueList);
        }

        return expense;
    }
}
