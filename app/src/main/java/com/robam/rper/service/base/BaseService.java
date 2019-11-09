package com.robam.rper.service.base;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.robam.rper.activity.MyApplication;

public abstract class BaseService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();

        MyApplication.getInstance().notifyCreate(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        MyApplication.getInstance().notifyDestroy(this);
    }
}
