package com.example.backend.mapper;

import com.example.backend.entity.RepaymentRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RepaymentRecordMapper {
    void insertRepaymentRecord(RepaymentRecord repaymentRecord);
    List<RepaymentRecord> getRecordsByLoanId(Long loanId);
}
