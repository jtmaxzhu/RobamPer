package com.robam.rper.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.os.Handler;

import com.robam.rper.R;
import com.robam.rper.service.SPService;
import com.robam.rper.tools.BackgroundExecutor;
import com.robam.rper.util.FileUtils;
import com.robam.rper.util.MiscUtil;
import com.robam.rper.util.PermissionUtil;
import com.robam.rper.util.StringUtil;

import java.util.Arrays;


public class LoadingActivity extends BaseActivity {
    private static final String TAG = LoadingActivity.class.getSimpleName();
    private static volatile boolean appInt = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        //sd卡新建monkey日志存放文件夹
    }

    /**
     * 写权限后续步骤
     */
    private void afterWritePermission() {
        FileUtils.getRperDir();
        // 已经初始化完毕过了，直接进入主页
        if (MyApplication.getInstance().hasFinishInit()) {
            //启动页面延时1.5s
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(LoadingActivity.this, IndexActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 2000);
        } else {
            // 新启动进闪屏页2s
            waitForAppInitialize();
        }
    }

    /**
     * 等待Launcher初始化完毕
     */
    private void waitForAppInitialize() {
      BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while (!MyApplication.getInstance().hasFinishInit()) {
                    MiscUtil.sleep(2000);
                }

                // 主线程跳转下
                MyApplication.getInstance().runOnUiThread(new Runnable() {
                    public void run() {
                        Intent intent = new Intent(LoadingActivity.this,
                                IndexActivity.class);
                        startActivity(intent);
                        LoadingActivity.this.finish();
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //如果内存有相关文件夹存在说明权限已经获取,无需再次申请
            if (!StringUtil.equals(SPService.getString(SPService.KEY_RPER_PATH_NAME, "rper"), "rper")) {
                afterWritePermission();
                return;
            }


            PermissionUtil.requestPermissions(Arrays.asList(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    this, new PermissionUtil.OnPermissionCallback(){

                        @Override
                        public void onPermissionResult(boolean result, String reason) {
                            if(result){
                                afterWritePermission();
                            }else{
                                // 再申请一次
                                PermissionUtil.requestPermissions(Arrays.asList(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), LoadingActivity.this, new PermissionUtil.OnPermissionCallback() {
                                    @Override
                                    public void onPermissionResult(boolean result, String reason) {
                                        // 如果申请失败
                                        if (!result) {
                                            FileUtils.fallBackToExternalDir(LoadingActivity.this);
                                        }
                                        afterWritePermission();
                                    }
                                });
                            }

                        }
                    });


        }else{
            afterWritePermission();
        }
    }


}
