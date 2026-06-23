package com.example.expense_split.repo;

import com.example.expense_split.model.UserRequest;
import com.example.expense_split.model.UserRequestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRequestRepository extends JpaRepository<UserRequest, UserRequestId> {
}
