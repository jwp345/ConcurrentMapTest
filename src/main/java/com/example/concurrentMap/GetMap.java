package com.example.concurrentMap;


import static com.example.concurrentMap.ModifyMap.cMap;


public class GetMap {
    public String get() {
        synchronized (cMap) {
            return cMap.get("3");
        }
    }
}
