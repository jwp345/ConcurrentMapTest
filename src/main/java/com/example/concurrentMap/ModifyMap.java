package com.example.concurrentMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ModifyMap {
    public static final Map<String, String> cMap = new ConcurrentHashMap<>();
    public static final Lock lock = new ReentrantLock();

    public ModifyMap() {
        cMap.put("1", "1");
        cMap.put("2", "2");
    }

    public void clearAndPut() {
        lock.lock();
        try {
            cMap.clear();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            cMap.put("3", "3");
            cMap.put("4", "4");
        } finally {
            lock.unlock();
        }
    }
}
