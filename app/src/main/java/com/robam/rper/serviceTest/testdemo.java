package com.robam.rper.serviceTest;

import com.robam.rper.activity.MyApplication;
import com.robam.rper.annotation.Param;
import com.robam.rper.annotation.Provider;
import com.robam.rper.injector.InjectorService;

/**
 * author : liuxiaohu
 * date   : 2019/11/22 10:54
 * desc   :
 * version: 1.0
 */
public class testdemo {
    public testdemo() {
        InjectorService injectorService = MyApplication.getInstance().findServiceByName(InjectorService.class.getName());
        injectorService.register(this);
    }

    @Provider(value = {@Param(value = "testdemoPro")})
    public void testdemoPro(){
        System.out.println("111");
    }

    void testdemoProdefault(){
        System.out.println("111");
    }

    private void testdemoProprivate(){
        System.out.println("111");
    }
}
