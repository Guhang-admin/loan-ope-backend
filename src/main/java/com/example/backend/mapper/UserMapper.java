package com.example.backend.mapper;

import com.example.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    List<User> getAllUsers();
    User getUserById(Long id);
    User getUserByUsername(String username);
    void updateCreditScore(@Param("id") Long id, @Param("creditScore") Integer creditScore);
    int getTotalUsers();
}