package com.example.expense_split.controller;

import com.example.expense_split.dto.AddExpenseRequest;
import com.example.expense_split.model.Expense;
import com.example.expense_split.model.User;
import com.example.expense_split.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expense")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/add-expense")
    public ResponseEntity<Expense> addExpense(
            @RequestBody AddExpenseRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Expense createdExpense = expenseService.addExpense(request, currentUser);
        return ResponseEntity.ok(createdExpense);
    }
}
