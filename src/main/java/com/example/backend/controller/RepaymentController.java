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
@RequestMapping("/api/repayments")
public class RepaymentController {
    @Autowired
    private RepaymentService repaymentService;

    @GetMapping("/plans/{loanId}")
    public ResponseEntity<List<RepaymentPlan>> getPlansByLoanId(@PathVariable Long loanId) {
        List<RepaymentPlan> plans = repaymentService.getPlansByLoanId(loanId);
        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    @GetMapping("/records/{loanId}")
    public ResponseEntity<List<RepaymentRecord>> getRecordsByLoanId(@PathVariable Long loanId) {
        List<RepaymentRecord> records = repaymentService.getRecordsByLoanId(loanId);
        return new ResponseEntity<>(records, HttpStatus.OK);
    }

    @PostMapping("/pay")
    public ResponseEntity<RepaymentRecord> makePayment(@RequestParam Long planId, @RequestParam String paymentMethod) {
        RepaymentRecord record = repaymentService.makePayment(planId, paymentMethod);
        if (record != null) {
            return new ResponseEntity<>(record, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
