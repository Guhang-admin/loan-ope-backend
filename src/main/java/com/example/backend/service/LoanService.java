package com.example.backend.service;

import com.example.backend.entity.Loan;
import com.example.backend.entity.RepaymentPlan;
import com.example.backend.repository.LoanRepository;
import com.example.backend.repository.RepaymentPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class LoanService {
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private RepaymentPlanRepository repaymentPlanRepository;

    public Loan applyLoan(Loan loan) {
        loan.setLoanDate(new Date());
        loan.setStatus("申请中");
        Loan savedLoan = loanRepository.save(loan);
        // 生成还款计划
        generateRepaymentPlan(savedLoan);
        return savedLoan;
    }

    private void generateRepaymentPlan(Loan loan) {
        BigDecimal loanAmount = loan.getLoanAmount();
        BigDecimal monthlyRate = loan.getInterestRate().divide(new BigDecimal(100)).divide(new BigDecimal(12));
        int loanTerm = loan.getLoanTerm();
        
        // 计算每月还款额（等额本息）
        BigDecimal monthlyPayment = loanAmount.multiply(monthlyRate).multiply(
            monthlyRate.add(BigDecimal.ONE).pow(loanTerm)
        ).divide(
            monthlyRate.add(BigDecimal.ONE).pow(loanTerm).subtract(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP
        );
        
        BigDecimal remainingPrincipal = loanAmount;
        for (int i = 1; i <= loanTerm; i++) {
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal principal = monthlyPayment.subtract(interest).setScale(2, BigDecimal.ROUND_HALF_UP);
            
            // 最后一期调整本金，确保总和正确
            if (i == loanTerm) {
                principal = remainingPrincipal;
                monthlyPayment = principal.add(interest);
            }
            
            RepaymentPlan plan = new RepaymentPlan();
            plan.setLoanId(loan.getId());
            plan.setInstallmentNumber(i);
            plan.setDueDate(new Date(System.currentTimeMillis() + (long) i * 30 * 24 * 60 * 60 * 1000));
            plan.setPrincipal(principal);
            plan.setInterest(interest);
            plan.setTotalAmount(monthlyPayment);
            plan.setStatus("未还款");
            
            repaymentPlanRepository.save(plan);
            remainingPrincipal = remainingPrincipal.subtract(principal);
        }
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public Loan getLoanById(Long id) {
        return loanRepository.findById(id).orElse(null);
    }
}
