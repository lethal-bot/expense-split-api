package com.example.expense_split.utililty;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class HelperMethods {
    public static double splitAmount(double totalAmount, int totalPeople) {
        return BigDecimal
                .valueOf(totalAmount)
                .divide(BigDecimal.valueOf(totalPeople), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
