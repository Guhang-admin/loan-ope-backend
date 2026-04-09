package com.example.backend.repository;

import com.example.backend.entity.RepaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentPlanRepository extends JpaRepository<RepaymentPlan, Long> {
    List<RepaymentPlan> findByLoanId(Long loanId);
}