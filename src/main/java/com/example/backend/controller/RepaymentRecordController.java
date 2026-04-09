package com.example.backend.controller;

import com.example.backend.entity.RepaymentRecord;
import com.example.backend.service.RepaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repayment-records")
public class RepaymentRecordController {
    @Autowired
    private RepaymentService repaymentService;

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<RepaymentRecord>> getRecordsByLoanId(@PathVariable Long loanId) {
        List<RepaymentRecord> records = repaymentService.getRecordsByLoanId(loanId);
        return new ResponseEntity<>(records, HttpStatus.OK);
    }
}