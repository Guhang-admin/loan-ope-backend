package com.example.backend.examples.memoryleak;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 第3期：内存异常 - 内存泄漏问题
 * 错误代码：静态集合持有对象引用导致内存泄漏
 */
public class MemoryLeakExample {
    
    /**
     * 错误的实现：静态集合导致内存泄漏
     */
    public static class BadUserCache {
        // 问题：静态集合导致内存泄漏
        private static final Map<Long, User> userCache = new ConcurrentHashMap<>();
        
        public void addUser(User user) {
            userCache.put(user.getId(), user);
        }
        
        public User getUser(Long id) {
            return userCache.get(id);
        }
        
        // 问题：没有清理机制
        public void clear() {
            // 空实现
        }
        
        public int size() {
            return userCache.size();
        }
    }
    
    /**
     * 正确的实现：带有过期机制的缓存
     */
    public static class GoodUserCache {
        private final Map<Long, User> userCache = new ConcurrentHashMap<>();
        private final Map<Long, Long> lastAccessTime = new ConcurrentHashMap<>();
        private static final long EXPIRY_TIME = 3600000; // 1小时
        
        public void addUser(User user) {
            userCache.put(user.getId(), user);
            lastAccessTime.put(user.getId(), System.currentTimeMillis());
        }
        
        public User getUser(Long id) {
            User user = userCache.get(id);
            if (user != null) {
                lastAccessTime.put(id, System.currentTimeMillis());
            }
            return user;
        }
        
        // 定期清理过期数据
        public void cleanup() {
            long now = System.currentTimeMillis();
            List<Long> toRemove = new ArrayList<>();
            for (Map.Entry<Long, Long> entry : lastAccessTime.entrySet()) {
                if (now - entry.getValue() > EXPIRY_TIME) {
                    toRemove.add(entry.getKey());
                }
            }
            for (Long id : toRemove) {
                userCache.remove(id);
                lastAccessTime.remove(id);
            }
        }
        
        public void clear() {
            userCache.clear();
            lastAccessTime.clear();
        }
        
        public int size() {
            return userCache.size();
        }
    }
    
    public static class EventManager {
        private List<EventListener> listeners = new ArrayList<>();
        
        public void addListener(EventListener listener) {
            listeners.add(listener);
        }
        
        // 问题：没有移除监听器的方法
        public void removeListener(EventListener listener) {
            listeners.remove(listener);
        }
        
        public void notifyListeners(Event event) {
            for (EventListener listener : listeners) {
                listener.onEvent(event);
            }
        }
        
        public int getListenerCount() {
            return listeners.size();
        }
    }
    
    public static class User {
        private Long id;
        private String name;
        private byte[] data; // 大对象
        
        public User(Long id, String name) {
            this.id = id;
            this.name = name;
            // 问题：创建大对象
            this.data = new byte[1024 * 1024]; // 1MB
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
    }
    
    public static interface EventListener {
        void onEvent(Event event);
    }
    
    public static class Event {
        private String type;
        private Object data;
        
        public Event(String type, Object data) {
            this.type = type;
            this.data = data;
        }
        
        public String getType() { return type; }
        public Object getData() { return data; }
    }
}