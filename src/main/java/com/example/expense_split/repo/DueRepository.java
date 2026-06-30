package com.example.expense_split.repo;

import com.example.expense_split.model.Due;
import com.example.expense_split.model.Group;
import com.example.expense_split.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface DueRepository extends JpaRepository<Due, Long> {
    Optional<Due> findByUserWhichWillGetAndUserWhichWillGiveAndGroup(User get, User give, Group group);
    boolean existsByUserWhichWillGetAndUserWhichWillGiveAndGroup(User get, User give, Group group);

    @Query("SELECT d FROM Due d WHERE d.group = :group AND (d.userWhichWillGet = :user OR d.userWhichWillGive = :user)")
    List<Due> findByGroupAndUserInvolved(@Param("group") Group group, @Param("user") User user);
}
