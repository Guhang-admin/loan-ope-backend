package com.example.backend.controller;

import com.example.backend.examples.concurrency.ConcurrencyExample;
import com.example.backend.examples.dataconsistency.TransactionExample;
import com.example.backend.examples.memoryleak.MemoryLeakExample;
import com.example.backend.examples.timeout.ConnectionLeakExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 案例演示控制器
 * 用于前端与后端案例的交互
 */
@RestController
@RequestMapping("/api/examples")
public class ExamplesController {
    
    @Autowired
    private ConnectionLeakExample connectionLeakExample;
    
    // 用于演示连接池问题的计数器
    private AtomicInteger connectionCount = new AtomicInteger(0);
    
    // 用于演示内存泄漏的缓存
    private final Map<Long, Object> leakyCache = new HashMap<>();
    
    // 用于演示并发问题的库存
    private AtomicInteger inventory = new AtomicInteger(100);
    
    /**
     * 第1期：神秘超时 - 模拟连接泄漏
     */
    @GetMapping("/timeout/leak")
    public Map<String, Object> simulateConnectionLeak() {
        // 实际调用ConnectionLeakExample中的getUserById方法，模拟连接泄漏
        int count = connectionCount.incrementAndGet();
        try {
            // 调用有问题的方法，导致连接泄漏
            connectionLeakExample.getUserById(1L);
        } catch (Exception e) {
            // 忽略异常，重点演示连接泄漏
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "模拟连接泄漏");
        response.put("connectionCount", count);
        response.put("warning", "连接未关闭，可能导致连接池耗尽");
        return response;
    }
    
    @GetMapping("/timeout/fixed")
    public Map<String, Object> fixedConnectionLeak() {
        // 实际调用ConnectionLeakExample中的getUserByIdFixed方法，正确关闭连接
        try {
            // 调用修复后的方法，正确关闭连接
            connectionLeakExample.getUserByIdFixed(1L);
        } catch (Exception e) {
            // 忽略异常，重点演示连接关闭
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "连接正确关闭");
        response.put("connectionCount", connectionCount.get());
        response.put("info", "使用 try-with-resources 自动关闭连接");
        return response;
    }
    
    // 用于演示数据不一致的账户余额
    private Map<Long, BigDecimal> accountBalances = new HashMap<>();
    
    /**
     * 初始化账户余额
     */
    @PostMapping("/dataconsistency/reset")
    public Map<String, Object> resetAccounts() {
        accountBalances.clear();
        accountBalances.put(1001L, new BigDecimal(1000));
        accountBalances.put(1002L, new BigDecimal(500));
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "账户余额已初始化");
        response.put("accounts", accountBalances);
        return response;
    }
    
    /**
     * 增加账户余额
     */
    @PostMapping("/dataconsistency/add-balance")
    public Map<String, Object> addBalance(@RequestParam Long userId,
                                        @RequestParam BigDecimal amount) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 初始化默认账户余额
            if (accountBalances.isEmpty()) {
                accountBalances.put(1001L, new BigDecimal(1000));
                accountBalances.put(1002L, new BigDecimal(500));
            }
            
            BigDecimal currentBalance = accountBalances.getOrDefault(userId, BigDecimal.ZERO);
            BigDecimal newBalance = currentBalance.add(amount);
            accountBalances.put(userId, newBalance);
            
            response.put("status", "success");
            response.put("message", "余额增加成功");
            response.put("userId", userId);
            response.put("addedAmount", amount);
            response.put("previousBalance", currentBalance);
            response.put("currentBalance", newBalance);
            response.put("currentBalances", accountBalances);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "增加余额失败");
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    /**
     * 获取账户余额
     */
    @GetMapping("/dataconsistency/accounts")
    public Map<String, Object> getAccounts() {
        Map<String, Object> response = new HashMap<>();
        // 初始化默认账户余额
        if (accountBalances.isEmpty()) {
            accountBalances.put(1001L, new BigDecimal(1000));
            accountBalances.put(1002L, new BigDecimal(500));
        }
        response.put("status", "success");
        response.put("accounts", accountBalances);
        return response;
    }
    
    /**
     * 第2期：数据不一致 - 模拟事务问题
     */
    @PostMapping("/dataconsistency/transfer")
    public Map<String, Object> simulateTransfer(@RequestParam Long fromUserId,
                                              @RequestParam Long toUserId,
                                              @RequestParam BigDecimal amount) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 初始化默认账户余额
            if (accountBalances.isEmpty()) {
                accountBalances.put(1001L, new BigDecimal(1000));
                accountBalances.put(1002L, new BigDecimal(500));
            }
            
            // 获取操作前的余额
            BigDecimal fromBalanceBefore = accountBalances.getOrDefault(fromUserId, BigDecimal.ZERO);
            BigDecimal toBalanceBefore = accountBalances.getOrDefault(toUserId, BigDecimal.ZERO);
            
            // 模拟没有事务的转账
            // 1. 扣减转出账户余额
            if (fromBalanceBefore.compareTo(amount) >= 0) {
                accountBalances.put(fromUserId, fromBalanceBefore.subtract(amount));
                
                // 2. 模拟异常：故意不更新转入账户余额
                // 这里模拟异常情况，导致数据不一致
                
                // 3. 获取操作后的余额
                BigDecimal fromBalanceAfter = accountBalances.get(fromUserId);
                BigDecimal toBalanceAfter = toBalanceBefore; // 转入账户余额未更新
                
                response.put("status", "success");
                response.put("message", "转账操作执行（无事务）");
                response.put("warning", "没有事务管理，可能导致数据不一致");
                
                // 添加数据变更前后的信息
                Map<String, Object> balanceChanges = new HashMap<>();
                balanceChanges.put("fromUserId", fromUserId);
                balanceChanges.put("toUserId", toUserId);
                balanceChanges.put("amount", amount);
                balanceChanges.put("fromBalanceBefore", fromBalanceBefore);
                balanceChanges.put("fromBalanceAfter", fromBalanceAfter);
                balanceChanges.put("toBalanceBefore", toBalanceBefore);
                balanceChanges.put("toBalanceAfter", toBalanceAfter);
                balanceChanges.put("inconsistent", true);
                
                response.put("balanceChanges", balanceChanges);
                response.put("currentBalances", accountBalances);
            } else {
                response.put("status", "error");
                response.put("message", "余额不足");
                response.put("fromBalance", fromBalanceBefore);
                response.put("requiredAmount", amount);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "转账失败");
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    @PostMapping("/dataconsistency/transfer-fixed")
    @Transactional
    public Map<String, Object> fixedTransfer(@RequestParam Long fromUserId,
                                           @RequestParam Long toUserId,
                                           @RequestParam BigDecimal amount) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 初始化默认账户余额
            if (accountBalances.isEmpty()) {
                accountBalances.put(1001L, new BigDecimal(1000));
                accountBalances.put(1002L, new BigDecimal(500));
            }
            
            // 获取操作前的余额
            BigDecimal fromBalanceBefore = accountBalances.getOrDefault(fromUserId, BigDecimal.ZERO);
            BigDecimal toBalanceBefore = accountBalances.getOrDefault(toUserId, BigDecimal.ZERO);
            
            // 模拟有事务的转账
            // 1. 检查余额是否充足
            if (fromBalanceBefore.compareTo(amount) >= 0) {
                // 2. 扣减转出账户余额
                accountBalances.put(fromUserId, fromBalanceBefore.subtract(amount));
                
                // 3. 增加转入账户余额
                accountBalances.put(toUserId, toBalanceBefore.add(amount));
                
                // 4. 获取操作后的余额
                BigDecimal fromBalanceAfter = accountBalances.get(fromUserId);
                BigDecimal toBalanceAfter = accountBalances.get(toUserId);
                
                response.put("status", "success");
                response.put("message", "转账操作执行（有事务）");
                response.put("info", "使用 @Transactional 确保事务性");
                
                // 添加数据变更前后的信息
                Map<String, Object> balanceChanges = new HashMap<>();
                balanceChanges.put("fromUserId", fromUserId);
                balanceChanges.put("toUserId", toUserId);
                balanceChanges.put("amount", amount);
                balanceChanges.put("fromBalanceBefore", fromBalanceBefore);
                balanceChanges.put("fromBalanceAfter", fromBalanceAfter);
                balanceChanges.put("toBalanceBefore", toBalanceBefore);
                balanceChanges.put("toBalanceAfter", toBalanceAfter);
                balanceChanges.put("inconsistent", false);
                
                response.put("balanceChanges", balanceChanges);
                response.put("currentBalances", accountBalances);
            } else {
                response.put("status", "error");
                response.put("message", "余额不足");
                response.put("fromBalance", fromBalanceBefore);
                response.put("requiredAmount", amount);
            }
        } catch (Exception e) {
            // 事务会自动回滚，恢复账户余额
            response.put("status", "error");
            response.put("message", "转账失败（事务已回滚）");
            response.put("error", e.getMessage());
            response.put("currentBalances", accountBalances);
        }
        return response;
    }
    
    /**
     * 第3期：内存异常 - 模拟内存泄漏
     */
    @PostMapping("/memoryleak/add")
    public Map<String, Object> addToLeakyCache(@RequestParam Long id,
                                             @RequestParam String data) {
        // 模拟内存泄漏：向静态缓存添加数据
        leakyCache.put(id, data);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "数据添加到缓存");
        response.put("cacheSize", leakyCache.size());
        response.put("warning", "缓存没有清理机制，可能导致内存泄漏");
        return response;
    }
    
    @GetMapping("/memoryleak/status")
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("cacheSize", leakyCache.size());
        response.put("info", "缓存大小持续增长，可能导致内存溢出");
        return response;
    }
    
    @PostMapping("/memoryleak/clear")
    public Map<String, Object> clearCache() {
        leakyCache.clear();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "缓存已清理");
        response.put("cacheSize", leakyCache.size());
        response.put("info", "定期清理缓存可以防止内存泄漏");
        return response;
    }
    
    /**
     * 第4期：并发陷阱 - 模拟竞态条件
     */
    @PostMapping("/concurrency/order")
    public Map<String, Object> processOrder(@RequestParam int quantity) {
        // 模拟竞态条件
        int current = inventory.get();
        Map<String, Object> response = new HashMap<>();
        
        if (current >= quantity) {
            // 模拟处理时间
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 这里存在竞态条件
            int newInventory = inventory.addAndGet(-quantity);
            response.put("status", "success");
            response.put("message", "订单处理完成");
            response.put("inventory", newInventory);
            response.put("warning", "存在竞态条件，可能导致库存错误");
        } else {
            response.put("status", "error");
            response.put("message", "库存不足");
            response.put("inventory", current);
        }
        return response;
    }
    
    @PostMapping("/concurrency/order-fixed")
    public Map<String, Object> processOrderFixed(@RequestParam int quantity) {
        // 使用原子操作避免竞态条件
        Map<String, Object> response = new HashMap<>();
        
        while (true) {
            int current = inventory.get();
            if (current < quantity) {
                response.put("status", "error");
                response.put("message", "库存不足");
                response.put("inventory", current);
                return response;
            }
            if (inventory.compareAndSet(current, current - quantity)) {
                response.put("status", "success");
                response.put("message", "订单处理完成（原子操作）");
                response.put("inventory", current - quantity);
                response.put("info", "使用 CAS 操作避免竞态条件");
                return response;
            }
            // 竞争失败，重试
        }
    }
    
    @GetMapping("/concurrency/inventory")
    public Map<String, Object> getInventory() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("inventory", inventory.get());
        return response;
    }
    
    @PostMapping("/concurrency/reset")
    public Map<String, Object> resetInventory() {
        inventory.set(100);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "库存已重置");
        response.put("inventory", inventory.get());
        return response;
    }
}
