package com.example.backend.examples.concurrency;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 第4期：并发陷阱 - 竞态条件与死锁问题
 * 错误代码：竞态条件和死锁风险
 */
public class ConcurrencyExample {
    
    /**
     * 错误的实现：竞态条件
     */
    public static class BadOrderService {
        private int inventory = 100; // 共享资源
        
        public void processOrder(int quantity) {
            // 问题：竞态条件
            if (inventory >= quantity) {
                // 模拟处理时间
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                inventory -= quantity;
                System.out.println("Order processed, remaining inventory: " + inventory);
            } else {
                System.out.println("Insufficient inventory");
            }
        }
        
        // 问题：死锁风险
        public synchronized void transferResources(BadOrderService other, int amount) {
            synchronized (this) {
                synchronized (other) {
                    if (this.inventory >= amount) {
                        this.inventory -= amount;
                        other.inventory += amount;
                        System.out.println("Transfer completed");
                    }
                }
            }
        }
        
        public int getInventory() {
            return inventory;
        }
    }
    
    /**
     * 正确的实现：使用原子类和避免死锁
     */
    public static class GoodOrderService {
        private AtomicInteger inventory = new AtomicInteger(100); // 使用原子类
        private final Lock lock = new ReentrantLock();
        
        public void processOrder(int quantity) {
            // 使用锁避免竞态条件
            lock.lock();
            try {
                if (inventory.get() >= quantity) {
                    // 模拟处理时间
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    inventory.addAndGet(-quantity);
                    System.out.println("Order processed, remaining inventory: " + inventory.get());
                } else {
                    System.out.println("Insufficient inventory");
                }
            } finally {
                lock.unlock();
            }
        }
        
        // 避免死锁：固定锁顺序
        public void transferResources(GoodOrderService other, int amount) {
            GoodOrderService first = this.hashCode() < other.hashCode() ? this : other;
            GoodOrderService second = this.hashCode() < other.hashCode() ? other : this;
            
            synchronized (first) {
                synchronized (second) {
                    if (this.inventory.get() >= amount) {
                        this.inventory.addAndGet(-amount);
                        other.inventory.addAndGet(amount);
                        System.out.println("Transfer completed");
                    }
                }
            }
        }
        
        // 使用原子操作的方法
        public void processOrderAtomic(int quantity) {
            while (true) {
                int current = inventory.get();
                if (current < quantity) {
                    System.out.println("Insufficient inventory");
                    return;
                }
                if (inventory.compareAndSet(current, current - quantity)) {
                    System.out.println("Order processed, remaining inventory: " + inventory.get());
                    return;
                }
                // 竞争失败，重试
            }
        }
        
        public int getInventory() {
            return inventory.get();
        }
    }
}