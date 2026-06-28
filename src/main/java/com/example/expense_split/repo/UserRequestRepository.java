package com.example.expense_split.repo;

import com.example.expense_split.model.RequestStatus;
import com.example.expense_split.model.UserRequest;
import com.example.expense_split.model.UserRequestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRequestRepository extends JpaRepository<UserRequest, UserRequestId> {
    List<UserRequest> findByUserUserIdAndStatus(Long userId, RequestStatus status);
}
