package com.example.expense_split.repo;

import com.example.expense_split.model.Due;
import com.example.expense_split.model.Group;
import com.example.expense_split.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DueRepository extends JpaRepository<Due, Long> {
    Optional<Due> findByUserWhichWillGetAndUserWhichWillGiveAndGroup(User get, User give, Group group);
    boolean existsByUserWhichWillGetAndUserWhichWillGiveAndGroup(User get, User give, Group group);
}
