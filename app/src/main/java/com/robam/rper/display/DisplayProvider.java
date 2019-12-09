package com.robam.rper.display;

import android.content.Context;
import android.view.Display;


import com.robam.rper.annotation.LocalService;
import com.robam.rper.display.items.base.DisplayItem;
import com.robam.rper.display.items.base.Displayable;
import com.robam.rper.display.items.base.RecordPattern;
import com.robam.rper.injector.param.Subscriber;
import com.robam.rper.service.base.ExportService;
import com.robam.rper.util.ClassUtil;
import com.robam.rper.util.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * author : liuxiaohu
 * date   : 2019/11/9 8:23
 * desc   :
 * version: 1.0
 */
@LocalService
public class DisplayProvider implements ExportService {

    private static final String TAG = "DisplayProvider";

    public static final int DISPLAY_MODE = 0;
    public static final int RECORDING_MODE = 1;

    private Map<String, DisplayItemInfo> allDisplayItems;

    private Map<String, DisplayWrapper> runningDisplay;

    private Map<String, String> cachedContent;

    private ScheduledExecutorService scheduExecutor;

    private ExecutorService executorService;

    private volatile int currentMode = 0;

    private volatile boolean isRunning = false;

    private static long REFRESH_PERIOD = 500;

    private AtomicBoolean startRefresh = new AtomicBoolean(false);

    private volatile boolean pauseFlag = false;


    @Override
    public void onCreate(Context context) {
        this.allDisplayItems = loadDisplayItem();
        runningDisplay = new ConcurrentHashMap<>();
        this.scheduExecutor = Executors.newSingleThreadScheduledExecutor();
        this.executorService = Executors.newCachedThreadPool();
        this.cachedContent = new ConcurrentHashMap<>();


    }

    @Override
    public void onDestroy(Context context) {
        for (String name : runningDisplay.keySet()){
            DisplayWrapper wrapper = runningDisplay.get(name);
            wrapper.reference.stop();
        }
        runningDisplay.clear();
        runningDisplay = null;
        if (this.scheduExecutor != null && !this.scheduExecutor.isShutdown()){
            this.scheduExecutor.shutdown();
        }
        this.scheduExecutor = null;
        if (this.executorService != null && !this.executorService.isShutdown()) {
            this.executorService.shutdownNow();
        }
        this.executorService = null;
    }

    /**
     * 获取显示项列表
     * @return
     */
    public List<DisplayItemInfo> getAllDisplayItems(){
        //按照名称排序
        allDisplayItems.keySet();
        ArrayList<String> list = new ArrayList<>(allDisplayItems.keySet());
        Collections.sort(list);
        List<DisplayItemInfo> displayItems = new ArrayList<>(list.size()+1);
        for (String key: list) {
            displayItems.add(allDisplayItems.get(key));
        }
        return displayItems;
    }

    /**
     * 获取正在运行列表
     * @return
     */
    public Set<String> getRunningDisplayItems() {
        // 按照名称排序
        return runningDisplay.keySet();
    }

    /**
     * 获取显示项列表
     * @return
     */
    public String getDisplayContent(String name) {
        return cachedContent.get(name);
    }

    /**
     * 触发特定项
     * @param name
     * @return
     */
    public boolean triggerItem(String name){
        DisplayWrapper wrapper = runningDisplay.get(name);
        if (wrapper != null) {
            wrapper.trigger();
            return true;
        } else {
            return false;
        }
    }

    /** 定时刷新启动器 */
    private Runnable task = new Runnable() {
        @Override
        public void run() {
            if (runningDisplay.size() == 0){
                startRefresh.set(false);
                return;
            }
            //定时500ms后执行
            scheduExecutor.schedule(this, REFRESH_PERIOD, TimeUnit.MILLISECONDS);

            //正在运行，或者暂停中，不进行操作
            if (isRunning || pauseFlag){
                return;
            }
            isRunning = true;
            for (Map.Entry<String, DisplayWrapper> entry : runningDisplay.entrySet()){
                DisplayWrapper wrapper = entry.getValue();
                if (wrapper.isRunning){
                    continue;
                }
                if (executorService != null && !executorService.isShutdown()){
                    executorService.execute(getDisplayRunnable(entry.getKey()));
                }
            }
            isRunning = false;
        }
    };


    /***
     * 获取任务执行器
     * @param name 小工具名称
     * @return 执行器
     */
    private Runnable getDisplayRunnable(final String name){
        return new Runnable() {
            @Override
            public void run() {
                if (pauseFlag){
                    return;
                }
                DisplayWrapper wrapper = runningDisplay.get(name);
                if (wrapper == null){
                    return;
                }
                switch (currentMode){
                    case DISPLAY_MODE:
                        cachedContent.put(name, wrapper.getContent());
                        break;
                    case RECORDING_MODE:
                        // 录制模式，通知显示工具记录数据
                        wrapper.record();
                        break;
                }
            }
        };
    }


    /**
     * 通过工具类与参数反射生成显示工具并配置参数
     * 工具类需事先 {@link Displayable} 接口，并对需要注入的依赖实现public的设置方法，并在相关方法使用{@link Subscriber}注解
     *
     * @param name 工具类名称
     * @return 显示名称与显示工具
     */
    public boolean startDisplay(String name){
        DisplayItemInfo displayItemInfo = allDisplayItems.get(name);
        if (displayItemInfo == null){
            //加载空信息
            return false;
        }
        if (runningDisplay.containsKey(name)){
            //显示项正在运行不需要启动
            return true;
        }

        Displayable displayable = null;
        try{
            displayable = ClassUtil.constructClass(displayItemInfo.getTargetClass());
            displayable.start();
            DisplayWrapper wrapper = new DisplayWrapper(displayable);
            runningDisplay.put(name, wrapper);
            if (!startRefresh.getAndSet(true)){
                scheduExecutor.schedule(task, 500, TimeUnit.MILLISECONDS);
            }
            return true;
        }catch (Exception e){
            if (displayable != null) {
                displayable.stop();
            }
            LogUtil.e(TAG, "构造显示项抛出异常", e);
        }
        return false;
    }

    /**
     * 停止特定项
     * @param name
     */
    public void stopDisplay(String name){
        DisplayWrapper info = runningDisplay.remove(name);
        if (info != null){
            info.reference.stop();
        }
    }

    /**
     * 停止所有显示项
     */
    public void stopAllDisplay(){
        for (String name:runningDisplay.keySet()){
            DisplayWrapper wrapper = runningDisplay.get(name);
            wrapper.reference.stop();
        }
        runningDisplay.clear();
    }

    /**
     * 开始录制
     */
    public void startRecording(){
        pauseFlag = true;
        for (DisplayWrapper wrapper : runningDisplay.values()){
            wrapper.startRecord();
        }
        this.currentMode = RECORDING_MODE;
        pauseFlag = false;
    }

    /**
     * 停止录制
     * @return
     */

    public Map<RecordPattern, List<RecordPattern.RecordItem>> stopRecording(){
        pauseFlag = true;
        this.currentMode = DISPLAY_MODE;
        //强制停止
        executorService.shutdownNow();
        Map<RecordPattern, List<RecordPattern.RecordItem>> result = new HashMap<>();
        for (DisplayWrapper wrapper : runningDisplay.values()){
            result.putAll(wrapper.stopRecord());
        }
        executorService = Executors.newCachedThreadPool();
        pauseFlag = false;
        return result;
    }


    /**
     * 加载所有显示项
     * @return
     */
    private Map<String, DisplayItemInfo> loadDisplayItem(){
        List<Class<? extends Displayable>> allDisplayable = ClassUtil.findSubClass(Displayable.class, DisplayItem.class);
        if (allDisplayable != null && allDisplayable.size() > 0){
            Map<String, DisplayItemInfo> infoMap = new HashMap<>(allDisplayable.size()+1);
            //加载类信息
            for (Class<? extends Displayable> clazz : allDisplayable){
                DisplayItem annotation = clazz.getAnnotation(DisplayItem.class);
                if (annotation != null){
                    DisplayItemInfo info = new DisplayItemInfo(annotation, clazz);
                    DisplayItemInfo origin = infoMap.get(info.getName());
                    if (origin == null){
                        infoMap.put(info.getName(), info);
                    }else {
                        if (origin.level < info.level){
                            infoMap.put(info.getName(), info);
                        }
                    }

                }
            }
            return infoMap;
        }

        return null;
    }



    public static class DisplayWrapper{
        public long lastCallTime = 0L;
        private String previousContent;
        private Displayable reference;
        private long maxSpendTime = 0;
        private final long minSpendTime;
        private int smallCount = 0;
        private volatile boolean isRunning = false;

        public DisplayWrapper(Displayable reference) {
            this.reference = reference;
            this.minSpendTime = reference.getRefreshFrequency();
            this.maxSpendTime = minSpendTime;
        }

        public void trigger() {
            reference.trigger();
        }

        public void startRecord() {
            reference.startRecord();
        }

        public Map<RecordPattern, List<RecordPattern.RecordItem>> stopRecord(){
            isRunning = true;
            Map<RecordPattern, List<RecordPattern.RecordItem>> records = reference.stopRecord();
            isRunning = false;
            return records;
        }

        public String getContent(){
            if (isRunning){
                return  previousContent;
            }
            //自动降速
            if (System.currentTimeMillis() - lastCallTime < maxSpendTime){
                return  previousContent;
            }
            long startTime = System.currentTimeMillis();
            isRunning = true;
            lastCallTime = startTime;
            try {
                previousContent = reference.getCurrentInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
            isRunning = false;

            //一次调用时间
            long spendTime = System.currentTimeMillis() - startTime;
            LogUtil.d(TAG,"调用【%s】耗时%dms", reference.getClass().getSimpleName(), spendTime);
            if (spendTime > maxSpendTime){
                maxSpendTime = spendTime;
                smallCount = 0;
            }else if (spendTime < maxSpendTime/2){
                smallCount ++ ;
                if (smallCount >=2){
                    maxSpendTime = minSpendTime;
                }
            }
            return  previousContent;
        }

        public void record(){
            if (isRunning) {
                return;
            }

            // 自动降速
            if (System.currentTimeMillis() - lastCallTime < maxSpendTime) {
                return;
            }

            long startTime = System.currentTimeMillis();
            lastCallTime = startTime;
            isRunning = true;
            try {
                reference.record();
            } catch (Throwable t) {
                LogUtil.e(TAG, t, "调用Displayable【%s】record抛出异常", reference);
            }
            isRunning = false;

            // 一次调用时间
            long spendTime = System.currentTimeMillis() - startTime;
            if (spendTime > maxSpendTime) {
                maxSpendTime = spendTime;
                smallCount = 0;

                // 小于一半
            } else if (spendTime < maxSpendTime / 2) {
                smallCount ++;

                if (smallCount >= 2) {
                    maxSpendTime = minSpendTime;
                }
            }
        }
    }
}
