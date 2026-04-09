package com.example.backend.service;

import com.example.backend.entity.RepaymentPlan;
import com.example.backend.entity.RepaymentRecord;
import com.example.backend.repository.RepaymentPlanRepository;
import com.example.backend.repository.RepaymentRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class RepaymentService {
    @Autowired
    private RepaymentPlanRepository repaymentPlanRepository;
    @Autowired
    private RepaymentRecordRepository repaymentRecordRepository;

    public List<RepaymentPlan> getPlansByLoanId(Long loanId) {
        return repaymentPlanRepository.findByLoanId(loanId);
    }

    public List<RepaymentRecord> getRecordsByLoanId(Long loanId) {
        return repaymentRecordRepository.findByLoanId(loanId);
    }

    public RepaymentRecord makePayment(Long planId, String paymentMethod) {
        RepaymentPlan plan = repaymentPlanRepository.findById(planId).orElse(null);
        if (plan == null) {
            return null;
        }

        // 创建还款记录
        RepaymentRecord record = new RepaymentRecord();
        record.setLoanId(plan.getLoanId());
        record.setPlanId(planId);
        record.setPaymentDate(new Date());
        record.setAmount(plan.getTotalAmount());
        record.setPaymentMethod(paymentMethod);

        // 更新还款计划状态
        plan.setStatus("已还款");
        repaymentPlanRepository.save(plan);

        return repaymentRecordRepository.save(record);
    }
}
