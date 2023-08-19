package com.example.concurrentMap;

import java.util.concurrent.TimeUnit;

import static com.example.concurrentMap.ModifyMap.cMap;
import static com.example.concurrentMap.ModifyMap.lock;

public class GetMap {
    public String get() {
        try {
            lock.tryLock(3, TimeUnit.SECONDS);
            return cMap.get("3");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
