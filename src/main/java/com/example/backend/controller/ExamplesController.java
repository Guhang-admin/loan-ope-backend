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
    public Map<String, Object> addToLeakyCache(@RequestBody Map<String, Object> request) {
        Long id = Long.valueOf(request.get("id").toString());
        String data = request.get("data").toString();
        
        // 模拟内存泄漏：向静态缓存添加数据
        leakyCache.put(id, data);
        
        // 获取内存使用信息
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "数据添加到缓存");
        response.put("cacheSize", leakyCache.size());
        response.put("memoryUsage", String.format("%.2f MB / %.2f MB (%.1f%%)", 
            usedMemory / (1024.0 * 1024.0), 
            maxMemory / (1024.0 * 1024.0), 
            memoryUsagePercent));
        response.put("objectCount", leakyCache.size());
        response.put("warning", "缓存没有清理机制，可能导致内存泄漏");
        return response;
    }
    
    @GetMapping("/memoryleak/status")
    public Map<String, Object> getCacheStatus() {
        // 获取内存使用信息
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("cacheSize", leakyCache.size());
        response.put("memoryUsage", String.format("%.2f MB / %.2f MB (%.1f%%)", 
            usedMemory / (1024.0 * 1024.0), 
            maxMemory / (1024.0 * 1024.0), 
            memoryUsagePercent));
        response.put("objectCount", leakyCache.size());
        response.put("info", "缓存大小持续增长，可能导致内存溢出");
        return response;
    }
    
    @PostMapping("/memoryleak/clear")
    public Map<String, Object> clearCache() {
        leakyCache.clear();
        
        // 触发垃圾回收
        System.gc();
        
        // 获取内存使用信息
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "缓存已清理");
        response.put("cacheSize", leakyCache.size());
        response.put("memoryUsage", String.format("%.2f MB / %.2f MB (%.1f%%)", 
            usedMemory / (1024.0 * 1024.0), 
            maxMemory / (1024.0 * 1024.0), 
            memoryUsagePercent));
        response.put("objectCount", leakyCache.size());
        response.put("info", "定期清理缓存可以防止内存泄漏");
        return response;
    }
    
    @PostMapping("/memoryleak/add-multiple")
    public Map<String, Object> addMultipleToCache(@RequestParam int count) {
        // 批量添加数据到缓存，加速内存泄漏
        for (int i = 0; i < count; i++) {
            long id = System.currentTimeMillis() + i;
            // 创建较大的数据
            String data = "模拟内存泄漏数据" + i;
            StringBuilder largeData = new StringBuilder();
            for (int j = 0; j < 10000; j++) {
                largeData.append(data);
            }
            leakyCache.put(id, largeData.toString());
        }
        
        // 获取内存使用信息
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "批量数据添加到缓存");
        response.put("cacheSize", leakyCache.size());
        response.put("addedCount", count);
        response.put("memoryUsage", String.format("%.2f MB / %.2f MB (%.1f%%)", 
            usedMemory / (1024.0 * 1024.0), 
            maxMemory / (1024.0 * 1024.0), 
            memoryUsagePercent));
        response.put("objectCount", leakyCache.size());
        response.put("warning", "批量添加数据加速内存泄漏过程");
        return response;
    }
    
    /**
     * 第4期：并发陷阱 - 模拟竞态条件
     */
    @PostMapping("/concurrency/order")
    public Map<String, Object> processOrder(@RequestParam int quantity) {
        // 模拟多个线程造成的竞态条件
        Map<String, Object> response = new HashMap<>();
        
        // 先获取当前库存
        int initialInventory = inventory.get();
        response.put("initialInventory", initialInventory);
        
        if (initialInventory < quantity * 2) {
            response.put("status", "error");
            response.put("message", "库存不足，需要至少" + (quantity * 2) + "个库存来模拟竞态条件");
            response.put("inventory", initialInventory);
            response.put("details", "当前库存为" + initialInventory + "，无法满足两个线程各处理" + quantity + "个订单的需求");
            return response;
        }
        
        // 创建两个线程模拟竞态条件
        Thread thread1 = new Thread(() -> {
            // 线程1：读取库存后sleep
            int current = inventory.get();
            System.out.println("Thread 1: 读取库存 = " + current);
            try {
                // 模拟处理时间，让线程2有机会也读取到相同的库存
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 非原子操作：基于读取的旧值进行扣减
            // 这里模拟竞态条件：线程1使用的是它之前读取的current值，而不是最新的值
            int newInventory = current - quantity;
            inventory.set(newInventory);
            System.out.println("Thread 1: 扣减后库存 = " + newInventory);
        });
        
        Thread thread2 = new Thread(() -> {
            // 线程2：直接处理，不sleep
            try {
                // 稍等一下，确保线程1先读取库存
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int current = inventory.get();
            System.out.println("Thread 2: 读取库存 = " + current);
            // 非原子操作：基于读取的旧值进行扣减
            // 这里模拟竞态条件：线程2使用的是它之前读取的current值，而不是最新的值
            int newInventory = current - quantity;
            inventory.set(newInventory);
            System.out.println("Thread 2: 扣减后库存 = " + newInventory);
        });
        
        // 启动线程
        thread1.start();
        thread2.start();
        
        // 等待线程完成
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 计算最终库存
        int finalInventory = inventory.get();
        int expectedInventory = initialInventory - quantity * 2;
        boolean hasRaceCondition = finalInventory != expectedInventory;
        
        response.put("status", "success");
        response.put("message", "竞态条件模拟完成");
        response.put("inventory", finalInventory);
        response.put("expectedInventory", expectedInventory);
        response.put("raceConditionOccurred", hasRaceCondition);
        response.put("warning", hasRaceCondition ? "竞态条件已发生！库存计算错误" : "竞态条件未发生，库存计算正确");
        response.put("details", "两个线程同时处理订单，线程1读取库存后sleep，线程2直接处理，可能导致两个线程都读取到相同的库存值");
        response.put("example", "初始库存: " + initialInventory + "，两个线程各处理" + quantity + "个订单，预期库存: " + expectedInventory + "，实际库存: " + finalInventory);
        response.put("explanation", hasRaceCondition ? "两个线程都读取到" + initialInventory + "，都扣减" + quantity + "，最终库存变成" + finalInventory + "，而不是预期的" + expectedInventory : "两个线程依次处理，库存计算正确");
        
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
                response.put("details", "当前库存为" + current + "，无法满足订单数量" + quantity);
                return response;
            }
            if (inventory.compareAndSet(current, current - quantity)) {
                response.put("status", "success");
                response.put("message", "订单处理完成（原子操作）");
                response.put("inventory", current - quantity);
                response.put("info", "使用 CAS 操作避免竞态条件");
                response.put("details", "使用 compareAndSet 方法确保原子性，避免竞态条件");
                response.put("example", "即使多个线程同时处理订单，CAS 操作也能保证库存计算正确");
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
        response.put("details", "当前库存状态，可用于检查并发操作后的结果");
        return response;
    }
    
    @PostMapping("/concurrency/reset")
    public Map<String, Object> resetInventory() {
        inventory.set(100);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "库存已重置");
        response.put("inventory", inventory.get());
        response.put("details", "库存已重置为100，可用于重新测试并发操作");
        return response;
    }
    
    /**
     * 模拟死锁
     */
    @PostMapping("/concurrency/deadlock")
    public Map<String, Object> simulateDeadlock() {
        Map<String, Object> response = new HashMap<>();
        
        // 模拟死锁场景
        final Object lock1 = new Object();
        final Object lock2 = new Object();
        
        Thread thread1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("Thread 1: 获得 lock1");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Thread 1: 尝试获得 lock2");
                synchronized (lock2) {
                    System.out.println("Thread 1: 获得 lock2");
                }
            }
        });
        
        Thread thread2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("Thread 2: 获得 lock2");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Thread 2: 尝试获得 lock1");
                synchronized (lock1) {
                    System.out.println("Thread 2: 获得 lock1");
                }
            }
        });
        
        thread1.start();
        thread2.start();
        
        try {
            // 等待线程执行一段时间
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        response.put("status", "warning");
        response.put("message", "死锁模拟完成");
        response.put("details", "两个线程互相等待对方持有的锁，导致死锁");
        response.put("example", "Thread 1 持有 lock1 等待 lock2，Thread 2 持有 lock2 等待 lock1");
        response.put("threads", "线程1状态: " + thread1.getState() + ", 线程2状态: " + thread2.getState());
        response.put("solution", "按固定顺序获取锁，避免嵌套锁，使用 tryLock 超时机制");
        
        return response;
    }
}
