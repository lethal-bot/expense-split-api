package com.example.expense_split;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ExpenseSplitApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExpenseSplitApplication.class, args);
	}

}

