package com.robam.rper.injector.cache;

import com.robam.rper.injector.param.InjectParam;
import com.robam.rper.injector.param.RunningThread;
import com.robam.rper.injector.provider.ParamReference;

import java.lang.reflect.Method;

/**
 * author : liuxiaohu
 * date   : 2019/8/9 10:29
 * desc   :
 * version: 1.0
 */
public class InjectParamMeta {
    private Method targetMethod;
    private InjectParam paramType;
    private RunningThread thread;

    public InjectParamMeta(Method targetMethod, InjectParam paramType, RunningThread thread) {
        this.targetMethod = targetMethod;
        this.paramType = paramType;
        this.thread = thread;
    }

    public void addToReference(ParamReference reference, Object item) {
        if (!reference.addReference(item, targetMethod, thread)) {
            throw new RuntimeException(String.format("添加引用失败，reference=%s，target=%s", reference, item));
        }
    }

    public InjectParam getParamType() {
        return paramType;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }
}
