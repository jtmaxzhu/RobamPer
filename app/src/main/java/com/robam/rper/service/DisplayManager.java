package com.robam.rper.service;

import com.robam.rper.display.DisplayProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * author : liuxiaohu
 * date   : 2019/11/13 13:54
 * desc   :
 * version: 1.0
 */
public class DisplayManager {

    private static final String TAG = "DisplayManager";

    public static final String STOP_DISPLAY = "stopDisplay";

    private static DisplayManager instance;

    /**
     * 获取显示控制实例
     * @return
     */
    public static  DisplayManager getInstance(){
        if (instance == null){
            instance = new DisplayManager();
        }
        return instance;
    }

    private DisplayProvider provider;
    private ScheduledExecutorService scheduledService;
    private PerFloatService.OnRunListener runListener;

    private DisplayManager(){


    }





}
