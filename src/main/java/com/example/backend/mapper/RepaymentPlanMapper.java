package com.example.backend.mapper;

import com.example.backend.entity.RepaymentPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RepaymentPlanMapper {
    void insertRepaymentPlan(RepaymentPlan repaymentPlan);
    List<RepaymentPlan> getPlansByLoanId(Long loanId);
    void updatePlanStatus(@Param("id") Long id, @Param("status") String status);
}
