package com.example.banking_transaction_engine.account.entity;

import com.example.banking_transaction_engine.account.enums.AccountStatus;
import com.example.banking_transaction_engine.account.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Production engines use un-guessable unique strings for external facing account routes
    @Column(unique = true, nullable = false, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    // Precision 18, Scale 4 supports massive enterprise totals and fractional micro-fees
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version; // Optimistic locking guard clause for general read/writes

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO.setScale(4);
        }
        if (this.status == null) {
            this.status = AccountStatus.ACTIVE;
        }
    }
}
