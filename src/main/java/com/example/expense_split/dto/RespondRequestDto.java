package com.example.expense_split.dto;

import com.example.expense_split.model.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RespondRequestDto {
    private Long groupId;
    private RequestStatus status;
}
