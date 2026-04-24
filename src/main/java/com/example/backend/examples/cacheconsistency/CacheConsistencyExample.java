package com.example.backend.examples.cacheconsistency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高级案例：缓存一致性问题
 * 演示缓存与数据库之间的数据一致性问题
 */
public class CacheConsistencyExample {
    private static final Logger logger = LoggerFactory.getLogger(CacheConsistencyExample.class);
    
    // 模拟数据库
    private final Map<Long, Product> database = new ConcurrentHashMap<>();
    
    // 模拟缓存（Redis）
    private final Map<Long, Product> cache = new ConcurrentHashMap<>();
    
    // 缓存过期时间（毫秒）
    private final long cacheExpireTime = 5000;
    
    // 缓存创建时间
    private final Map<Long, Long> cacheCreateTime = new ConcurrentHashMap<>();
    
    // 操作计数器
    private final AtomicInteger operationCount = new AtomicInteger(0);
    
    public CacheConsistencyExample() {
        // 初始化测试数据
        database.put(1L, new Product(1L, "商品A", new BigDecimal("100.00"), 100));
        database.put(2L, new Product(2L, "商品B", new BigDecimal("200.00"), 50));
        database.put(3L, new Product(3L, "商品C", new BigDecimal("300.00"), 30));
    }
    
    /**
     * 模拟缓存穿透问题
     * @param productId 商品ID
     * @return 商品信息
     */
    public CacheResult simulateCachePenetration(Long productId) {
        CacheResult result = new CacheResult();
        result.setProductId(productId);
        result.setOperation("缓存穿透");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 从缓存获取
            Product cachedProduct = cache.get(productId);
            if (cachedProduct != null) {
                result.setFromCache(true);
                result.setProduct(cachedProduct);
                result.setMessage("从缓存获取数据");
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                return result;
            }
            
            // 缓存未命中，从数据库获取
            Product dbProduct = database.get(productId);
            if (dbProduct == null) {
                // 商品不存在，但仍然会查询数据库
                result.setFromDatabase(true);
                result.setMessage("商品不存在，但仍然查询了数据库");
                result.setWarning("缓存穿透：不存在的数据每次都查询数据库");
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                return result;
            }
            
            // 将数据放入缓存
            cache.put(productId, dbProduct);
            cacheCreateTime.put(productId, System.currentTimeMillis());
            
            result.setFromDatabase(true);
            result.setProduct(dbProduct);
            result.setMessage("从数据库获取数据并更新缓存");
            result.setInfo("已将数据放入缓存");
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            logger.error("缓存穿透模拟失败: {}", e.getMessage(), e);
            result.setError(e.getMessage());
            result.setMessage("操作失败");
        }
        
        return result;
    }
    
    /**
     * 模拟缓存击穿问题
     * @param productId 商品ID
     * @return 商品信息
     */
    public CacheResult simulateCacheBreakdown(Long productId) {
        CacheResult result = new CacheResult();
        result.setProductId(productId);
        result.setOperation("缓存击穿");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 模拟缓存过期
            cache.remove(productId);
            cacheCreateTime.remove(productId);
            
            // 模拟大量并发请求
            int concurrentRequests = 10;
            CountDownLatch latch = new CountDownLatch(concurrentRequests);
            AtomicInteger dbQueryCount = new AtomicInteger(0);
            
            for (int i = 0; i < concurrentRequests; i++) {
                new Thread(() -> {
                    try {
                        // 从缓存获取
                        Product cachedProduct = cache.get(productId);
                        if (cachedProduct != null) {
                            return;
                        }
                        
                        // 缓存未命中，从数据库获取
                        dbQueryCount.incrementAndGet();
                        Product dbProduct = database.get(productId);
                        
                        // 模拟数据库查询延迟
                        Thread.sleep(100);
                        
                        // 将数据放入缓存
                        cache.put(productId, dbProduct);
                        cacheCreateTime.put(productId, System.currentTimeMillis());
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
            
            latch.await(5, TimeUnit.SECONDS);
            
            result.setDbQueryCount(dbQueryCount.get());
            result.setMessage("缓存击穿模拟完成");
            result.setWarning("缓存击穿：" + dbQueryCount.get() + "个请求同时查询数据库");
            result.setInfo("应该使用互斥锁或提前刷新缓存");
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            logger.error("缓存击穿模拟失败: {}", e.getMessage(), e);
            result.setError(e.getMessage());
            result.setMessage("操作失败");
        }
        
        return result;
    }
    
    /**
     * 模拟缓存雪崩问题
     * @return 执行结果
     */
    public CacheResult simulateCacheAvalanche() {
        CacheResult result = new CacheResult();
        result.setOperation("缓存雪崩");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 模拟大量缓存同时过期
            cache.clear();
            cacheCreateTime.clear();
            
            // 模拟大量并发请求
            int concurrentRequests = 20;
            CountDownLatch latch = new CountDownLatch(concurrentRequests);
            AtomicInteger dbQueryCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            
            for (int i = 0; i < concurrentRequests; i++) {
                final Long productId = (long) (i % 3) + 1;
                new Thread(() -> {
                    try {
                        // 从缓存获取
                        Product cachedProduct = cache.get(productId);
                        if (cachedProduct != null) {
                            return;
                        }
                        
                        // 缓存未命中，从数据库获取
                        dbQueryCount.incrementAndGet();
                        Product dbProduct = database.get(productId);
                        
                        // 模拟数据库查询延迟
                        Thread.sleep(100);
                        
                        // 将数据放入缓存
                        cache.put(productId, dbProduct);
                        cacheCreateTime.put(productId, System.currentTimeMillis());
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        logger.error("请求失败: {}", e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
            
            latch.await(10, TimeUnit.SECONDS);
            
            result.setDbQueryCount(dbQueryCount.get());
            result.setErrorCount(errorCount.get());
            result.setMessage("缓存雪崩模拟完成");
            result.setWarning("缓存雪崩：" + dbQueryCount.get() + "个请求同时查询数据库，" + errorCount.get() + "个请求失败");
            result.setInfo("应该设置不同的缓存过期时间，避免同时过期");
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            logger.error("缓存雪崩模拟失败: {}", e.getMessage(), e);
            result.setError(e.getMessage());
            result.setMessage("操作失败");
        }
        
        return result;
    }
    
    /**
     * 模拟缓存一致性问题
     * @param productId 商品ID
     * @param newPrice 新价格
     * @return 执行结果
     */
    public CacheResult simulateCacheConsistency(Long productId, BigDecimal newPrice) {
        CacheResult result = new CacheResult();
        result.setProductId(productId);
        result.setOperation("缓存一致性");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 获取原始数据
            Product originalProduct = database.get(productId);
            Product cachedProduct = cache.get(productId);
            
            result.setOriginalPrice(originalProduct != null ? originalProduct.getPrice() : null);
            result.setCachedPrice(cachedProduct != null ? cachedProduct.getPrice() : null);
            
            // 更新数据库
            if (originalProduct != null) {
                originalProduct.setPrice(newPrice);
                database.put(productId, originalProduct);
            }
            
            // 模拟缓存更新延迟
            if (Math.random() < 0.5) {
                // 缓存更新失败
                result.setMessage("数据库更新成功，但缓存更新失败");
                result.setWarning("缓存不一致：数据库和缓存数据不一致");
                result.setError("缓存更新失败");
            } else {
                // 缓存更新成功
                if (cachedProduct != null) {
                    cachedProduct.setPrice(newPrice);
                    cache.put(productId, cachedProduct);
                }
                result.setMessage("数据库和缓存都更新成功");
                result.setInfo("数据一致性得到保证");
            }
            
            result.setNewPrice(newPrice);
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            logger.error("缓存一致性模拟失败: {}", e.getMessage(), e);
            result.setError(e.getMessage());
            result.setMessage("操作失败");
        }
        
        return result;
    }
    
    /**
     * 修复缓存穿透问题
     * @param productId 商品ID
     * @return 商品信息
     */
    public CacheResult fixCachePenetration(Long productId) {
        CacheResult result = new CacheResult();
        result.setProductId(productId);
        result.setOperation("修复缓存穿透");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 从缓存获取
            Product cachedProduct = cache.get(productId);
            if (cachedProduct != null) {
                result.setFromCache(true);
                result.setProduct(cachedProduct);
                result.setMessage("从缓存获取数据");
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                return result;
            }
            
            // 缓存未命中，从数据库获取
            Product dbProduct = database.get(productId);
            if (dbProduct == null) {
                // 商品不存在，将null值放入缓存，防止穿透
                cache.put(productId, null);
                cacheCreateTime.put(productId, System.currentTimeMillis());
                
                result.setFromDatabase(true);
                result.setMessage("商品不存在，已将null值放入缓存");
                result.setInfo("使用布隆过滤器或缓存null值防止穿透");
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                return result;
            }
            
            // 将数据放入缓存
            cache.put(productId, dbProduct);
            cacheCreateTime.put(productId, System.currentTimeMillis());
            
            result.setFromDatabase(true);
            result.setProduct(dbProduct);
            result.setMessage("从数据库获取数据并更新缓存");
            result.setInfo("已将数据放入缓存");
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            logger.error("修复缓存穿透失败: {}", e.getMessage(), e);
            result.setError(e.getMessage());
            result.setMessage("操作失败");
        }
        
        return result;
    }
    
    /**
     * 修复缓存击穿问题
     * @param productId 商品ID
     * @return 商品信息
     */
    public CacheResult fixCacheBreakdown(Long productId) {
        CacheResult result = new CacheResult();
        result.setProductId(productId);
        result.setOperation("修复缓存击穿");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 使用互斥锁
            String lockKey = "lock:product:" + productId;
            
            // 模拟获取锁
            boolean lockAcquired = true;
            
            if (lockAcquired) {
                try {
                    // 从缓存获取
                    Product cachedProduct = cache.get(productId);
                    if (cachedProduct != null) {
                        result.setFromCache(true);
                        result.setProduct(cachedProduct);
                        result.setMessage("从缓存获取数据");
                        result.setExecutionTime(System.currentTimeMillis() - startTime);
                        return result;
                    }
                    
                    // 缓存未命中，从数据库获取
                    Product dbProduct = database.get(productId);
                    
                    // 将数据放入缓存
                    cache.put(productId, dbProduct);
                    cacheCreateTime.put(productId, System.currentTimeMillis());
                    
                    result.setFromDatabase(true);
                    result.setProduct(dbProduct);
                    result.setMessage("从数据库获取数据并更新缓存");
                    result.setInfo("使用互斥锁防止缓存击穿");
                    result.setExecutionTime(System.currentTimeMillis() - startTime);
                    
                } finally {
                    // 释放锁
                    logger.info("释放锁: {}", lockKey);
                }
            } else {
                result.setMessage("获取锁失败，稍后重试");
                result.setInfo("使用互斥锁或提前刷新缓存");
                result.setExecutionTime(System.currentTimeMillis() - startTime);
            }
            
        } catch (Exception e) {
            logger.error("修复缓存击穿失败: {}", e.getMessage(), e);
            result.setError(e.getMessage());
            result.setMessage("操作失败");
        }
        
        return result;
    }
    
    /**
     * 修复缓存一致性问题
     * @param productId 商品ID
     * @param newPrice 新价格
     * @return 执行结果
     */
    public CacheResult fixCacheConsistency(Long productId, BigDecimal newPrice) {
        CacheResult result = new CacheResult();
        result.setProductId(productId);
        result.setOperation("修复缓存一致性");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 获取原始数据
            Product originalProduct = database.get(productId);
            Product cachedProduct = cache.get(productId);
            
            result.setOriginalPrice(originalProduct != null ? originalProduct.getPrice() : null);
            result.setCachedPrice(cachedProduct != null ? cachedProduct.getPrice() : null);
            
            // 更新数据库
            if (originalProduct != null) {
                originalProduct.setPrice(newPrice);
                database.put(productId, originalProduct);
            }
            
            // 更新缓存
            if (cachedProduct != null) {
                cachedProduct.setPrice(newPrice);
                cache.put(productId, cachedProduct);
            }
            
            // 使用延迟双删或消息队列确保一致性
            result.setMessage("数据库和缓存都更新成功");
            result.setInfo("使用延迟双删或消息队列保证一致性");
            result.setNewPrice(newPrice);
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            logger.error("修复缓存一致性失败: {}", e.getMessage(), e);
            result.setError(e.getMessage());
            result.setMessage("操作失败");
        }
        
        return result;
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        cacheCreateTime.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > cacheExpireTime
        );
        cache.keySet().removeIf(key -> !cacheCreateTime.containsKey(key));
        logger.info("已清理过期缓存");
    }
    
    /**
     * 重置所有数据
     */
    public void resetAll() {
        database.clear();
        cache.clear();
        cacheCreateTime.clear();
        operationCount.set(0);
        
        database.put(1L, new Product(1L, "商品A", new BigDecimal("100.00"), 100));
        database.put(2L, new Product(2L, "商品B", new BigDecimal("200.00"), 50));
        database.put(3L, new Product(3L, "商品C", new BigDecimal("300.00"), 30));
    }
    
    // 商品类
    public static class Product {
        private Long id;
        private String name;
        private BigDecimal price;
        private int stock;
        
        public Product(Long id, String name, BigDecimal price, int stock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.stock = stock;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public int getStock() { return stock; }
        public void setStock(int stock) { this.stock = stock; }
    }
    
    // 缓存结果类
    public static class CacheResult {
        private Long productId;
        private String operation;
        private Product product;
        private boolean fromCache;
        private boolean fromDatabase;
        private int dbQueryCount;
        private int errorCount;
        private BigDecimal originalPrice;
        private BigDecimal cachedPrice;
        private BigDecimal newPrice;
        private long executionTime;
        private String message;
        private String warning;
        private String info;
        private String error;
        
        // Getters and setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public Product getProduct() { return product; }
        public void setProduct(Product product) { this.product = product; }
        public boolean isFromCache() { return fromCache; }
        public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
        public boolean isFromDatabase() { return fromDatabase; }
        public void setFromDatabase(boolean fromDatabase) { this.fromDatabase = fromDatabase; }
        public int getDbQueryCount() { return dbQueryCount; }
        public void setDbQueryCount(int dbQueryCount) { this.dbQueryCount = dbQueryCount; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public BigDecimal getOriginalPrice() { return originalPrice; }
        public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
        public BigDecimal getCachedPrice() { return cachedPrice; }
        public void setCachedPrice(BigDecimal cachedPrice) { this.cachedPrice = cachedPrice; }
        public BigDecimal getNewPrice() { return newPrice; }
        public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
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