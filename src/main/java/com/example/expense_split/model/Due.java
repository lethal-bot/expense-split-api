package com.example.expense_split.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "dues")
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Due {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "due_id")
    private Long dueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_which_will_get", nullable = false)
    private User userWhichWillGet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_which_will_give", nullable = false)
    private User userWhichWillGive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "amount", nullable = false)
    private Double amount = 0.0;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreatedDate
    @Column(name = "created_date", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "modified_date", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime modifiedDate;
}
