package com.robam.rper.injector.cache;


import com.robam.rper.injector.param.InjectParam;
import com.robam.rper.library.Const;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * author : liuxiaohu
 * date   : 2019/8/9 10:29
 * desc   :
 * version: 1.0
 */
public class InjectParamTypeCache {
    private static InjectParamTypeCache cacheInstance;
    private Map<String, InjectParam> injectParamTypeList = new ConcurrentHashMap<>();

    public static InjectParamTypeCache getCacheInstance() {
        if (cacheInstance == null) {
            // 避免后续都得加锁
            synchronized (InjectParamTypeCache.class) {
                if (cacheInstance == null) {
                    cacheInstance = new InjectParamTypeCache();
                }
            }
        }

        return cacheInstance;
    }


    public void addCache(InjectParam cache) {
        injectParamTypeList.put(cache.getName(), cache);
    }

    public InjectParam getExistsParamType(String name) {
        return injectParamTypeList.get(name);
    }

    public InjectParam getExistsParamType(Class clazz) {
        String name;
        if (clazz.isPrimitive()) {
            name = Const.getPackedType(clazz).getName();
        } else {
            name = clazz.getName();
        }
        return injectParamTypeList.get(name);
    }
}
