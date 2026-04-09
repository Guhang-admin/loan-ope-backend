package com.example.backend.mapper;

import com.example.backend.entity.Loan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LoanMapper {
    void insertLoan(Loan loan);
    List<Loan> getAllLoans();
    Loan getLoanById(Long id);
    void updateLoanStatus(@Param("id") Long id, @Param("status") String status);
    List<Loan> getLoansByUserId(Long userId);
}
