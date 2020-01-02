package com.robam.rper.activity;

import android.support.v7.app.AppCompatActivity;

import com.robam.rper.R;
import com.robam.rper.activity.MyApplication;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.annotation.Param;
import com.robam.rper.annotation.Provider;
import com.robam.rper.injector.InjectorService;


/**
 * author : liuxiaohu
 * date   : 2019/11/22 10:54
 * desc   :
 * version: 1.0
 */
@EntryActivity(icon = R.drawable.xn, name = "测试页面", index = 4)
public class testdemo extends BaseActivity {
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


}
