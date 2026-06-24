package com.example.expense_split.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddExpenseRequest {
    private Long groupId;
    private String name;
    private Long totalExpenseAmount;
    private String descriptionOfExpense;
    private List<Long> friendIds;
}
