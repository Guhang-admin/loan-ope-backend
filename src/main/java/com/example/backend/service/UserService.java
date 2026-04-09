package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public List<User> getAllUsers() {
        return userMapper.getAllUsers();
    }

    public User getUserById(Long id) {
        return userMapper.getUserById(id);
    }

    public User getUserByUsername(String username) {
        return userMapper.getUserByUsername(username);
    }

    public User updateCreditScore(Long userId, Integer creditScore) {
        userMapper.updateCreditScore(userId, creditScore);
        return userMapper.getUserById(userId);
    }

    public int getTotalUsers() {
        return userMapper.getTotalUsers();
    }
}