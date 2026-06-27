package com.example.expense_split.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendRequestDto {
    private String email;
    private Long groupId;
}
