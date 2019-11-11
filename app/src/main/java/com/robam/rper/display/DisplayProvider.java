package com.robam.rper.display;

import android.content.Context;


import com.robam.rper.annotation.LocalService;
import com.robam.rper.display.items.base.DisplayItem;
import com.robam.rper.display.items.base.Displayable;
import com.robam.rper.display.items.base.RecordPattern;
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

    }

    /**
     * 获取显示项列表
     * @return
     */
    public List<DisplayItemInfo> getAllDisplayItems(){
        //按照名称排序
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

        }
    };

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
        HashMap<String,String> map = new HashMap<>();
        map.keySet();

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
