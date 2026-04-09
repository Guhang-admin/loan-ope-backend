package com.example.backend.controller;

import com.example.backend.service.FileService;
import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    @Autowired
    private FileService fileService;
    
    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFiles", fileService.getTotalFiles());
        stats.put("todayUploads", fileService.getTodayUploads());
        stats.put("totalUsers", userService.getTotalUsers());
        stats.put("status", "正常");
        return ResponseEntity.ok(stats);
    }
}