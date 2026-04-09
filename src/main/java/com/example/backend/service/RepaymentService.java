package com.example.backend.service;

import com.example.backend.entity.RepaymentPlan;
import com.example.backend.entity.RepaymentRecord;
import com.example.backend.mapper.RepaymentPlanMapper;
import com.example.backend.mapper.RepaymentRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class RepaymentService {
    @Autowired
    private RepaymentPlanMapper repaymentPlanMapper;
    @Autowired
    private RepaymentRecordMapper repaymentRecordMapper;

    public List<RepaymentPlan> getPlansByLoanId(Long loanId) {
        return repaymentPlanMapper.getPlansByLoanId(loanId);
    }

    public List<RepaymentRecord> getRecordsByLoanId(Long loanId) {
        return repaymentRecordMapper.getRecordsByLoanId(loanId);
    }

    public RepaymentRecord makePayment(Long planId, String paymentMethod) {
        // 这里简化处理，实际应该先查询还款计划
        RepaymentPlan plan = new RepaymentPlan();
        plan.setId(planId);
        plan.setLoanId(1L); // 实际应该从数据库查询
        plan.setTotalAmount(java.math.BigDecimal.valueOf(1000)); // 实际应该从数据库查询

        // 创建还款记录
        RepaymentRecord record = new RepaymentRecord();
        record.setLoanId(plan.getLoanId());
        record.setPlanId(planId);
        record.setPaymentDate(new Date());
        record.setAmount(plan.getTotalAmount());
        record.setPaymentMethod(paymentMethod);

        // 更新还款计划状态
        repaymentPlanMapper.updatePlanStatus(planId, "已还款");

        // 保存还款记录
        repaymentRecordMapper.insertRepaymentRecord(record);

        return record;
    }
}
