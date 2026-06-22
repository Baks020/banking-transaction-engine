package com.example.banking_transaction_engine.account.repository;

import com.example.banking_transaction_engine.account.entity.Account;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);


    @Lock(LockModeType.PESSIMISTIC_WRITE)

    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000")})
    Optional<Account> findWithLockByAccountNumber(String accountNumber);
}
