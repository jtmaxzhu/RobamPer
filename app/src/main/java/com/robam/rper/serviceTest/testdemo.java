package com.robam.rper.serviceTest;

import android.support.v7.app.AppCompatActivity;

import com.robam.rper.activity.MyApplication;
import com.robam.rper.annotation.Param;
import com.robam.rper.annotation.Provider;
import com.robam.rper.injector.InjectorService;
import com.robam.rper.injector.param.RunningThread;
import com.robam.rper.injector.param.SubscribeParamEnum;
import com.robam.rper.injector.param.Subscriber;
import com.robam.rper.service.DisplayManager;
import com.robam.rper.util.LogUtil;

/**
 * author : liuxiaohu
 * date   : 2019/11/22 10:54
 * desc   :
 * version: 1.0
 */
public class testdemo extends AppCompatActivity {
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

    @Subscriber(@Param(SubscribeParamEnum.APP_NAME))
    private void testdemoProprivate(final String appName){
        LogUtil.d("testdemo","1111111");
    }
}
