package com.example.backend.examples.distributedtransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 高级案例：分布式事务问题
 * 演示分布式环境下的数据一致性问题
 */
public class DistributedTransactionExample {
    private static final Logger logger = LoggerFactory.getLogger(DistributedTransactionExample.class);
    
    // 模拟数据库A：用户账户
    private final Map<Long, Account> databaseA = new ConcurrentHashMap<>();
    
    // 模拟数据库B：交易记录
    private final Map<Long, Transaction> databaseB = new ConcurrentHashMap<>();
    
    // 模拟外部支付系统
    private final Map<String, Payment> paymentSystem = new ConcurrentHashMap<>();
    
    // 事务ID生成器
    private long transactionId = 0;
    
    public DistributedTransactionExample() {
        // 初始化测试数据
        databaseA.put(1L, new Account(1L, "用户A", new BigDecimal("10000.00")));
        databaseA.put(2L, new Account(2L, "用户B", new BigDecimal("5000.00")));
        databaseA.put(3L, new Account(3L, "用户C", new BigDecimal("3000.00")));
    }
    
    /**
     * 模拟两阶段提交（2PC）失败场景
     * @param fromAccountId 转出账户ID
     * @param toAccountId 转入账户ID
     * @param amount 转账金额
     * @return 执行结果
     */
    public DistributedTransactionResult simulateTwoPhaseCommitFailure(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        DistributedTransactionResult result = new DistributedTransactionResult();
        result.setTransactionId(++transactionId);
        result.setFromAccountId(fromAccountId);
        result.setToAccountId(toAccountId);
        result.setAmount(amount);
        
        try {
            // 阶段1：准备阶段（Prepare）
            logger.info("两阶段提交 - 阶段1：准备开始");
            
            Account fromAccount = databaseA.get(fromAccountId);
            Account toAccount = databaseA.get(toAccountId);
            
            if (fromAccount == null || toAccount == null) {
                result.setSuccess(false);
                result.setMessage("账户不存在");
                result.setError("账户不存在");
                return result;
            }
            
            if (fromAccount.getBalance().compareTo(amount) < 0) {
                result.setSuccess(false);
                result.setMessage("余额不足");
                result.setError("余额不足");
                return result;
            }
            
            // 记录准备状态
            result.setFromAccountBefore(fromAccount.getBalance());
            result.setToAccountBefore(toAccount.getBalance());
            
            // 模拟准备阶段成功
            logger.info("两阶段提交 - 阶段1：准备成功");
            
            // 模拟网络故障或系统崩溃
            if (Math.random() < 0.3) {
                logger.error("两阶段提交 - 阶段1后发生故障");
                throw new RuntimeException("网络故障：准备阶段后系统崩溃");
            }
            
            // 阶段2：提交阶段（Commit）
            logger.info("两阶段提交 - 阶段2：提交开始");
            
            // 扣减转出账户
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            databaseA.put(fromAccountId, fromAccount);
            
            // 增加转入账户
            toAccount.setBalance(toAccount.getBalance().add(amount));
            databaseA.put(toAccountId, toAccount);
            
            // 创建交易记录
            Transaction transaction = new Transaction(
                result.getTransactionId(),
                fromAccountId,
                toAccountId,
                amount,
                "COMPLETED"
            );
            databaseB.put(result.getTransactionId(), transaction);
            
            result.setFromAccountAfter(fromAccount.getBalance());
            result.setToAccountAfter(toAccount.getBalance());
            result.setSuccess(true);
            result.setMessage("两阶段提交成功");
            result.setInfo("转账操作已完成");
            
            logger.info("两阶段提交 - 阶段2：提交成功");
            
        } catch (Exception e) {
            logger.error("两阶段提交失败: {}", e.getMessage(), e);
            
            // 回滚操作
            try {
                rollbackTransaction(result);
            } catch (Exception rollbackException) {
                logger.error("回滚失败: {}", rollbackException.getMessage(), rollbackException);
                result.setError("回滚失败: " + rollbackException.getMessage());
            }
            
            result.setSuccess(false);
            result.setMessage("两阶段提交失败");
            result.setError(e.getMessage());
            result.setWarning("分布式事务失败，可能导致数据不一致");
        }
        
        return result;
    }
    
    /**
     * 模拟TCC（Try-Confirm-Cancel）模式
     * @param fromAccountId 转出账户ID
     * @param toAccountId 转入账户ID
     * @param amount 转账金额
     * @return 执行结果
     */
    public DistributedTransactionResult simulateTCCPattern(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        DistributedTransactionResult result = new DistributedTransactionResult();
        result.setTransactionId(++transactionId);
        result.setFromAccountId(fromAccountId);
        result.setToAccountId(toAccountId);
        result.setAmount(amount);
        
        try {
            // Try阶段：资源预留
            logger.info("TCC模式 - Try阶段：资源预留开始");
            
            Account fromAccount = databaseA.get(fromAccountId);
            Account toAccount = databaseA.get(toAccountId);
            
            if (fromAccount == null || toAccount == null) {
                result.setSuccess(false);
                result.setMessage("账户不存在");
                result.setError("账户不存在");
                return result;
            }
            
            if (fromAccount.getBalance().compareTo(amount) < 0) {
                result.setSuccess(false);
                result.setMessage("余额不足");
                result.setError("余额不足");
                return result;
            }
            
            // 记录初始状态
            result.setFromAccountBefore(fromAccount.getBalance());
            result.setToAccountBefore(toAccount.getBalance());
            
            // 预留资金（冻结）
            BigDecimal frozenAmount = fromAccount.getFrozenAmount().add(amount);
            fromAccount.setFrozenAmount(frozenAmount);
            databaseA.put(fromAccountId, fromAccount);
            
            logger.info("TCC模式 - Try阶段：资源预留成功，冻结金额: {}", frozenAmount);
            
            // 模拟Try阶段失败
            if (Math.random() < 0.2) {
                logger.error("TCC模式 - Try阶段失败");
                throw new RuntimeException("Try阶段失败：资源预留失败");
            }
            
            // Confirm阶段：确认执行
            logger.info("TCC模式 - Confirm阶段：确认执行开始");
            
            // 扣减冻结金额
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            fromAccount.setFrozenAmount(fromAccount.getFrozenAmount().subtract(amount));
            databaseA.put(fromAccountId, fromAccount);
            
            // 增加转入账户
            toAccount.setBalance(toAccount.getBalance().add(amount));
            databaseA.put(toAccountId, toAccount);
            
            // 创建交易记录
            Transaction transaction = new Transaction(
                result.getTransactionId(),
                fromAccountId,
                toAccountId,
                amount,
                "CONFIRMED"
            );
            databaseB.put(result.getTransactionId(), transaction);
            
            result.setFromAccountAfter(fromAccount.getBalance());
            result.setToAccountAfter(toAccount.getBalance());
            result.setSuccess(true);
            result.setMessage("TCC模式执行成功");
            result.setInfo("转账操作已完成");
            
            logger.info("TCC模式 - Confirm阶段：确认执行成功");
            
        } catch (Exception e) {
            logger.error("TCC模式失败: {}", e.getMessage(), e);
            
            // Cancel阶段：取消操作
            try {
                cancelTransaction(result);
            } catch (Exception cancelException) {
                logger.error("Cancel阶段失败: {}", cancelException.getMessage(), cancelException);
                result.setError("Cancel失败: " + cancelException.getMessage());
            }
            
            result.setSuccess(false);
            result.setMessage("TCC模式执行失败");
            result.setError(e.getMessage());
            result.setWarning("TCC模式失败，需要手动处理");
        }
        
        return result;
    }
    
    /**
     * 模拟Saga模式
     * @param fromAccountId 转出账户ID
     * @param toAccountId 转入账户ID
     * @param amount 转账金额
     * @return 执行结果
     */
    public DistributedTransactionResult simulateSagaPattern(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        DistributedTransactionResult result = new DistributedTransactionResult();
        result.setTransactionId(++transactionId);
        result.setFromAccountId(fromAccountId);
        result.setToAccountId(toAccountId);
        result.setAmount(amount);
        
        try {
            logger.info("Saga模式 - 开始执行");
            
            Account fromAccount = databaseA.get(fromAccountId);
            Account toAccount = databaseA.get(toAccountId);
            
            if (fromAccount == null || toAccount == null) {
                result.setSuccess(false);
                result.setMessage("账户不存在");
                result.setError("账户不存在");
                return result;
            }
            
            if (fromAccount.getBalance().compareTo(amount) < 0) {
                result.setSuccess(false);
                result.setMessage("余额不足");
                result.setError("余额不足");
                return result;
            }
            
            // 记录初始状态
            result.setFromAccountBefore(fromAccount.getBalance());
            result.setToAccountBefore(toAccount.getBalance());
            
            // 步骤1：扣减转出账户
            logger.info("Saga模式 - 步骤1：扣减转出账户");
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            databaseA.put(fromAccountId, fromAccount);
            
            // 模拟步骤1失败
            if (Math.random() < 0.2) {
                logger.error("Saga模式 - 步骤1失败，开始回滚");
                // 补偿操作：恢复转出账户
                fromAccount.setBalance(fromAccount.getBalance().add(amount));
                databaseA.put(fromAccountId, fromAccount);
                throw new RuntimeException("步骤1失败：扣减账户失败");
            }
            
            // 步骤2：增加转入账户
            logger.info("Saga模式 - 步骤2：增加转入账户");
            toAccount.setBalance(toAccount.getBalance().add(amount));
            databaseA.put(toAccountId, toAccount);
            
            // 模拟步骤2失败
            if (Math.random() < 0.2) {
                logger.error("Saga模式 - 步骤2失败，开始回滚");
                // 补偿操作：恢复转入账户
                toAccount.setBalance(toAccount.getBalance().subtract(amount));
                databaseA.put(toAccountId, toAccount);
                // 补偿操作：恢复转出账户
                fromAccount.setBalance(fromAccount.getBalance().add(amount));
                databaseA.put(fromAccountId, fromAccount);
                throw new RuntimeException("步骤2失败：增加账户失败");
            }
            
            // 步骤3：创建交易记录
            logger.info("Saga模式 - 步骤3：创建交易记录");
            Transaction transaction = new Transaction(
                result.getTransactionId(),
                fromAccountId,
                toAccountId,
                amount,
                "COMPLETED"
            );
            databaseB.put(result.getTransactionId(), transaction);
            
            result.setFromAccountAfter(fromAccount.getBalance());
            result.setToAccountAfter(toAccount.getBalance());
            result.setSuccess(true);
            result.setMessage("Saga模式执行成功");
            result.setInfo("转账操作已完成");
            
            logger.info("Saga模式 - 执行成功");
            
        } catch (Exception e) {
            logger.error("Saga模式失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Saga模式执行失败");
            result.setError(e.getMessage());
            result.setWarning("Saga模式失败，已执行补偿操作");
        }
        
        return result;
    }
    
    /**
     * 回滚事务
     * @param result 事务结果
     */
    private void rollbackTransaction(DistributedTransactionResult result) {
        logger.info("开始回滚事务: {}", result.getTransactionId());
        
        // 回滚转出账户
        Account fromAccount = databaseA.get(result.getFromAccountId());
        if (fromAccount != null && result.getFromAccountAfter() != null) {
            fromAccount.setBalance(result.getFromAccountBefore());
            databaseA.put(result.getFromAccountId(), fromAccount);
        }
        
        // 回滚转入账户
        Account toAccount = databaseA.get(result.getToAccountId());
        if (toAccount != null && result.getToAccountAfter() != null) {
            toAccount.setBalance(result.getToAccountBefore());
            databaseA.put(result.getToAccountId(), toAccount);
        }
        
        // 删除交易记录
        databaseB.remove(result.getTransactionId());
        
        logger.info("事务回滚完成: {}", result.getTransactionId());
    }
    
    /**
     * 取消事务（TCC Cancel阶段）
     * @param result 事务结果
     */
    private void cancelTransaction(DistributedTransactionResult result) {
        logger.info("开始Cancel事务: {}", result.getTransactionId());
        
        // 取消冻结金额
        Account fromAccount = databaseA.get(result.getFromAccountId());
        if (fromAccount != null) {
            fromAccount.setFrozenAmount(BigDecimal.ZERO);
            databaseA.put(result.getFromAccountId(), fromAccount);
        }
        
        logger.info("事务Cancel完成: {}", result.getTransactionId());
    }
    
    /**
     * 获取账户信息
     * @param accountId 账户ID
     * @return 账户信息
     */
    public Account getAccount(Long accountId) {
        return databaseA.get(accountId);
    }
    
    /**
     * 获取交易记录
     * @param transactionId 交易ID
     * @return 交易记录
     */
    public Transaction getTransaction(Long transactionId) {
        return databaseB.get(transactionId);
    }
    
    /**
     * 重置所有数据
     */
    public void resetAll() {
        databaseA.clear();
        databaseB.clear();
        paymentSystem.clear();
        transactionId = 0;
        
        databaseA.put(1L, new Account(1L, "用户A", new BigDecimal("10000.00")));
        databaseA.put(2L, new Account(2L, "用户B", new BigDecimal("5000.00")));
        databaseA.put(3L, new Account(3L, "用户C", new BigDecimal("3000.00")));
    }
    
    // 账户类
    public static class Account {
        private Long id;
        private String name;
        private BigDecimal balance;
        private BigDecimal frozenAmount = BigDecimal.ZERO;
        
        public Account(Long id, String name, BigDecimal balance) {
            this.id = id;
            this.name = name;
            this.balance = balance;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
        public BigDecimal getFrozenAmount() { return frozenAmount; }
        public void setFrozenAmount(BigDecimal frozenAmount) { this.frozenAmount = frozenAmount; }
    }
    
    // 交易记录类
    public static class Transaction {
        private Long id;
        private Long fromAccountId;
        private Long toAccountId;
        private BigDecimal amount;
        private String status;
        
        public Transaction(Long id, Long fromAccountId, Long toAccountId, BigDecimal amount, String status) {
            this.id = id;
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.amount = amount;
            this.status = status;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
        public Long getToAccountId() { return toAccountId; }
        public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    // 支付记录类
    public static class Payment {
        private String id;
        private Long accountId;
        private BigDecimal amount;
        private String status;
        
        public Payment(String id, Long accountId, BigDecimal amount, String status) {
            this.id = id;
            this.accountId = accountId;
            this.amount = amount;
            this.status = status;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    // 分布式事务结果类
    public static class DistributedTransactionResult {
        private Long transactionId;
        private Long fromAccountId;
        private Long toAccountId;
        private BigDecimal amount;
        private BigDecimal fromAccountBefore;
        private BigDecimal fromAccountAfter;
        private BigDecimal toAccountBefore;
        private BigDecimal toAccountAfter;
        private boolean success;
        private String message;
        private String warning;
        private String info;
        private String error;
        
        // Getters and setters
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
        public Long getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
        public Long getToAccountId() { return toAccountId; }
        public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getFromAccountBefore() { return fromAccountBefore; }
        public void setFromAccountBefore(BigDecimal fromAccountBefore) { this.fromAccountBefore = fromAccountBefore; }
        public BigDecimal getFromAccountAfter() { return fromAccountAfter; }
        public void setFromAccountAfter(BigDecimal fromAccountAfter) { this.fromAccountAfter = fromAccountAfter; }
        public BigDecimal getToAccountBefore() { return toAccountBefore; }
        public void setToAccountBefore(BigDecimal toAccountBefore) { this.toAccountBefore = toAccountBefore; }
        public BigDecimal getToAccountAfter() { return toAccountAfter; }
        public void setToAccountAfter(BigDecimal toAccountAfter) { this.toAccountAfter = toAccountAfter; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getWarning() { return warning; }
        public void setWarning(String warning) { this.warning = warning; }
        public String getInfo() { return info; }
        public void setInfo(String info) { this.info = info; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}