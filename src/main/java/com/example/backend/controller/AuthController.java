package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> result = new HashMap<>();
        
        // 检查请求体是否为空
        if (credentials == null) {
            result.put("success", false);
            result.put("message", "请求参数不能为空");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
        
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        // 检查用户名和密码是否为空
        if (username == null || username.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "用户名不能为空");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
        
        if (password == null || password.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "密码不能为空");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
        
        User user = userService.getUserByUsername(username);
        
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);
        }
        
        if (!user.getPassword().equals(password)) {
            result.put("success", false);
            result.put("message", "密码错误");
            return new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);
        }
        
        result.put("success", true);
        result.put("message", "登录成功");
        result.put("user", user);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "退出成功");
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}