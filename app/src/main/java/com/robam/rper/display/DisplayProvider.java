package com.robam.rper.display;

import android.content.Context;


import com.robam.rper.display.items.base.Displayable;
import com.robam.rper.display.items.base.RecordPattern;
import com.robam.rper.service.base.ExportService;
import com.robam.rper.util.ClassUtil;
import com.robam.rper.util.LogUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * author : liuxiaohu
 * date   : 2019/11/9 8:23
 * desc   :
 * version: 1.0
 */
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


    @Override
    public void onCreate(Context context) {
//        this.allDisplayItems =

    }

    @Override
    public void onDestroy(Context context) {

    }

    /**
     * 加载所有显示项
     * @return
     */
    private Map<String, DisplayItemInfo> loadDisplayItem(){
        //List<Class<? extends Displayable>> allDisplayable = ClassUtil.findSubClass();
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
