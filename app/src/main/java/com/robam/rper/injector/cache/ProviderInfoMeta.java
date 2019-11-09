package com.robam.rper.injector.cache;



import com.robam.rper.injector.param.InjectParam;
import com.robam.rper.injector.provider.ProviderInfo;
import com.robam.rper.injector.provider.WeakInjectItem;

import java.lang.reflect.Method;
import java.util.List;

/**
 * author : liuxiaohu
 * date   : 2019/8/9 10:31
 * desc   :
 * version: 1.0
 */
public class ProviderInfoMeta {
    /**
     * 更新间隔
     */
    private long updatePeriod;

    /**
     * 提供参数
     */
    private List<InjectParam> provideParams;

    private boolean force;

    /**
     * 是否懒惰
     */
    private boolean lazy;

    private Method targetMethod;

    public ProviderInfo buildProvider() {
        return new ProviderInfo(updatePeriod, provideParams, lazy, force);
    }

    /**
     * 构建调用类
     * @param target
     * @return
     */
    public WeakInjectItem buildWeakInjectItem(Object target) {
        return new WeakInjectItem(targetMethod, target, provideParams);
    }

    public ProviderInfoMeta(long updatePeriod, List<InjectParam> provideParams, boolean lazy, boolean force, Method targetMethod) {
        this.updatePeriod = updatePeriod;
        this.provideParams = provideParams;
        this.lazy = lazy;
        this.force = force;
        this.targetMethod = targetMethod;
    }
}
