package com.example.backend.controller;

import com.example.backend.entity.RepaymentPlan;
import com.example.backend.entity.RepaymentRecord;
import com.example.backend.service.RepaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repayment-plans")
public class RepaymentController {
    @Autowired
    private RepaymentService repaymentService;

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<RepaymentPlan>> getPlansByLoanId(@PathVariable Long loanId) {
        List<RepaymentPlan> plans = repaymentService.getPlansByLoanId(loanId);
        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    @PutMapping("/{planId}/pay")
    public ResponseEntity<RepaymentRecord> makePayment(@PathVariable Long planId) {
        RepaymentRecord record = repaymentService.makePayment(planId, "银行卡");
        if (record != null) {
            return new ResponseEntity<>(record, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}