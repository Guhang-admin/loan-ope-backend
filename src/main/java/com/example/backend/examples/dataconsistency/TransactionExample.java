package com.example.backend.examples.dataconsistency;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第2期：数据不一致 - 事务管理问题
 * 错误代码：缺少事务控制导致数据不一致
 */
public class TransactionExample {
    private static final Logger logger = LoggerFactory.getLogger(TransactionExample.class);
    private UserRepository userRepository;
    private TransactionRepository transactionRepository;
    
    public TransactionExample(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * 错误的实现：缺少事务控制
     * 问题：部分操作失败时，已执行的操作不会回滚
     */
    public void transfer(Long fromUserId, Long toUserId, BigDecimal amount) {
        try {
            // 1. 扣减转出账户余额
            User fromUser = userRepository.findById(fromUserId);
            fromUser.setBalance(fromUser.getBalance().subtract(amount));
            userRepository.update(fromUser);
            
            // 2. 增加转入账户余额
            User toUser = userRepository.findById(toUserId);
            toUser.setBalance(toUser.getBalance().add(amount));
            userRepository.update(toUser);
            
            // 3. 创建交易记录
            Transaction transaction = new Transaction();
            transaction.setFromUserId(fromUserId);
            transaction.setToUserId(toUserId);
            transaction.setAmount(amount);
            transaction.setStatus("SUCCESS");
            transactionRepository.save(transaction);
        } catch (Exception e) {
            logger.error("Transfer error", e);
            // 问题：没有事务回滚，部分操作已执行
        }
    }
    
    /**
     * 正确的实现：使用 @Transactional 确保事务性
     */
    @Transactional
    public void transferFixed(Long fromUserId, Long toUserId, BigDecimal amount) {
        try {
            // 1. 扣减转出账户余额
            User fromUser = userRepository.findById(fromUserId);
            if (fromUser.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance");
            }
            fromUser.setBalance(fromUser.getBalance().subtract(amount));
            userRepository.update(fromUser);
            
            // 2. 增加转入账户余额
            User toUser = userRepository.findById(toUserId);
            toUser.setBalance(toUser.getBalance().add(amount));
            userRepository.update(toUser);
            
            // 3. 创建交易记录
            Transaction transaction = new Transaction();
            transaction.setFromUserId(fromUserId);
            transaction.setToUserId(toUserId);
            transaction.setAmount(amount);
            transaction.setStatus("SUCCESS");
            transactionRepository.save(transaction);
        } catch (Exception e) {
            logger.error("Transfer error", e);
            // 事务会自动回滚
            throw new TransferException("Transfer failed", e);
        }
    }
    
    public interface UserRepository {
        User findById(Long id);
        void update(User user);
    }
    
    public interface TransactionRepository {
        void save(Transaction transaction);
    }
    
    public static class User {
        private Long id;
        private BigDecimal balance;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
    }
    
    public static class Transaction {
        private Long fromUserId;
        private Long toUserId;
        private BigDecimal amount;
        private String status;
        
        public Long getFromUserId() { return fromUserId; }
        public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }
        public Long getToUserId() { return toUserId; }
        public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }
    
    public static class TransferException extends RuntimeException {
        public TransferException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}