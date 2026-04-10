package com.example.backend.examples.timeout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 第1期：神秘超时 - 数据库连接池超时问题
 * 错误代码：连接泄漏导致连接池耗尽
 */
public class ConnectionLeakExample {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionLeakExample.class);
    private DataSource dataSource;
    
    public ConnectionLeakExample(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * 错误的实现：连接泄漏
     * 问题：没有关闭连接、语句和结果集
     */
    public User getUserById(Long id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            String sql = "SELECT * FROM user WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPhone(rs.getString("phone"));
                return user;
            }
        } catch (SQLException e) {
            logger.error("Database error", e);
        }
        // 问题：没有关闭连接、语句和结果集
        return null;
    }
    
    /**
     * 正确的实现：使用 try-with-resources 自动关闭资源
     */
    public User getUserByIdFixed(Long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM user WHERE id = ?");) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Database error", e);
        }
        return null;
    }
    
    public static class User {
        private Long id;
        private String name;
        private String email;
        private String phone;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}