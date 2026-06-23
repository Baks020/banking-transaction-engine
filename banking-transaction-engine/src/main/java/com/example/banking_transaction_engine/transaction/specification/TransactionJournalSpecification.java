package com.example.banking_transaction_engine.transaction.specification;

import com.example.banking_transaction_engine.ledger.entity.TransactionJournal;
import com.example.banking_transaction_engine.transaction.dto.TransactionSearchRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class TransactionJournalSpecification {

    public static Specification<TransactionJournal> buildSpecification(TransactionSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filter by Account Number involvement (Matches either source OR destination)
            if (request.getAccountNumber() != null && !request.getAccountNumber().isBlank()) {
                Predicate sourceMatch = criteriaBuilder.equal(root.get("sourceAccountNumber"), request.getAccountNumber());
                Predicate destMatch = criteriaBuilder.equal(root.get("destinationAccountNumber"), request.getAccountNumber());
                predicates.add(criteriaBuilder.or(sourceMatch, destMatch));
            }

            // 2. Exact match search by Transaction Reference token
            if (request.getTransactionReference() != null && !request.getTransactionReference().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("transactionReference"), request.getTransactionReference()));
            }

            // 3. Filter by execution Status
            if (request.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }

            // 4. Bound search queries inside a strict Date Range window
            if (request.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), request.getStartDate()));
            }
            if (request.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), request.getEndDate()));
            }

            // Sort results by newest entries first natively in the query plan
            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
