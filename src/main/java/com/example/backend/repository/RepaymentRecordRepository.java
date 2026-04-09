package com.example.backend.repository;

import com.example.backend.entity.RepaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentRecordRepository extends JpaRepository<RepaymentRecord, Long> {
    List<RepaymentRecord> findByLoanId(Long loanId);
}