package com.robam.rper.injector.cache;

import java.util.List;

/**
 * author : liuxiaohu
 * date   : 2019/8/98:54
 * desc   :
 * version: 1.0
 */
public class ClassInfo {
    private List<ProviderInfoMeta> cachedProviderInfo;

    private List<InjectParamMeta> cachedInjectInfo;

    ClassInfo(List<ProviderInfoMeta> cachedProviderInfo, List<InjectParamMeta> cachedInjectInfo) {
        this.cachedProviderInfo = cachedProviderInfo;
        this.cachedInjectInfo = cachedInjectInfo;
    }

    public List<ProviderInfoMeta> getCachedProviderInfo() {
        return cachedProviderInfo;
    }

    public List<InjectParamMeta> getCachedInjectInfo() {
        return cachedInjectInfo;
    }
}
