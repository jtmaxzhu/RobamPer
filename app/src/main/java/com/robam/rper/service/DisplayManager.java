package com.robam.rper.service;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.robam.rper.R;
import com.robam.rper.activity.MyApplication;
import com.robam.rper.adapter.FloatWinAdapter;
import com.robam.rper.annotation.Param;
import com.robam.rper.annotation.Provider;
import com.robam.rper.display.DisplayItemInfo;
import com.robam.rper.display.DisplayProvider;
import com.robam.rper.display.items.base.RecordPattern;
import com.robam.rper.injector.InjectorService;
import com.robam.rper.tools.BackgroundExecutor;
import com.robam.rper.ui.RecycleViewDivider;
import com.robam.rper.util.ContextUtil;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.RecordUtil;
import com.robam.rper.util.StringUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * author : liuxiaohu
 * date   : 2019/11/13 13:54
 * desc   :
 * version: 1.0
 */
@Provider(@Param(value = DisplayManager.STOP_DISPLAY))
public class DisplayManager {

    private static final String TAG = "DisplayManager";

    public static final String STOP_DISPLAY = "stopDisplay";

    private static DisplayManager instance;

    private List<DisplayItemInfo> currentDisplayInfo = new ArrayList<>();
    private List<String> displayMessages = new ArrayList<>();

    private FloatWinAdapter floatWinAdapter;
    private int runningMode;
    private volatile boolean runningFlag = true;
    private ScheduledExecutorService executorService;

    private DisplayConnection connection;
    private RecyclerView floatWinList;

    public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;

    public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;
    private InjectorService injectorService;

    private DisplayProvider provider;
    private ScheduledExecutorService scheduledService;
    private PerFloatService.OnRunListener runListener;
    private PerFloatService.PerFloatBinder binder;
    private PerFloatService.OnStopListener stopListener = new PerFloatService.OnStopListener() {
        @Override
        public boolean onStopClick() {
            stop();
            return false;
        }
    };

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


    private DisplayManager(){
        provider = MyApplication.getInstance().findServiceByName(DisplayProvider.class.getName());
        executorService = Executors.newSingleThreadScheduledExecutor();
        runListener = new MyRunningListener(this);
    }


    /**
     * 停止数据显示
     */
    private void stop(){
        runningFlag = false;
        //停止服务
        MyApplication.getInstance().stopServiceByName(DisplayProvider.class.getName());
        if (connection != null){
            MyApplication.getContext().unbindService(connection);
            connection = null;
            binder = null;
        }

        //发送停止显示消息，去掉已经勾选的选项
        injectorService.pushMessage(STOP_DISPLAY, null, false);
        injectorService.unregister(this);
        this.displayMessages.clear();
        this.currentDisplayInfo.clear();
    }

    /**
     * 更新悬浮窗数据显示列表
     * @param newItems
     * @param removeItems
     * @return
     */

    public synchronized List<DisplayItemInfo> updateRecordingItems(List<DisplayItemInfo> newItems, List<DisplayItemInfo> removeItems){
        List<DisplayItemInfo> newInfos = new ArrayList<>(currentDisplayInfo);
        if (removeItems != null && removeItems.size() > 0){
            for (DisplayItemInfo remove : removeItems){
                provider.stopDisplay(remove.getName());
            }
            newInfos.removeAll(removeItems);
        }
        //添加显示项
        List<DisplayItemInfo> failed = new ArrayList<>();
        if (newItems != null && newItems.size() > 0){
            for (DisplayItemInfo info : newItems){
                boolean result = provider.startDisplay(info.getName());
                //如果失败，将失败项放入failed
                if (result){
                    newInfos.add(info);
                }else {
                    failed.add(info);
                }
            }

        }
        currentDisplayInfo = newInfos;
        //绑定服务
        if (connection == null && currentDisplayInfo.size() > 0){
            start();
        }else if (connection != null && currentDisplayInfo.size() == 0){
            stop();
        }
        return failed;
        /**内存数据
         * newItems={ArrayList} size=1 0={DisplayItemInfo} (level=1 , name="cpu" targetClass="class com.robam.rper.display.items.CPUTools")
         * removeItems=null
         * currentDisplayInfo={ArrayList@xxxx}(0=DisplayItemInfo@xxxx)
         */


    }



    /**
     * 数据显示
     */
    private void start(){
        connection = new DisplayConnection(this);
        Context context = MyApplication.getContext();
        injectorService = MyApplication.getInstance().findServiceByName(InjectorService.class.getName());

        runningFlag = true;

        Intent intent = new Intent(context, PerFloatService.class);
        //绑定服务
        Boolean b = context.bindService(new Intent(context, PerFloatService.class), connection, Context.BIND_AUTO_CREATE);
        LogUtil.d(TAG,"b:"+b);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                if (runningFlag){
                    executorService.schedule(this, 500, TimeUnit.MILLISECONDS);
                }
                //LogUtil.d(TAG,"runningFlag:"+runningFlag);
                updateDisplayInfo();
            }
        }, 500, TimeUnit.MILLISECONDS);

    }

    private void updateDisplayInfo(){
        if (runningMode == DisplayProvider.DISPLAY_MODE){
            displayMessages.clear();
            for (DisplayItemInfo info : currentDisplayInfo){
                displayMessages.add(provider.getDisplayContent(info.getName()));
            }

            MyApplication.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LogUtil.d(TAG,"floatWinAdapter"+floatWinAdapter);
                    floatWinAdapter.updateListViewSource(currentDisplayInfo, displayMessages);
                }
            });
        }
        /**内存数据
         * displayMessages{全局:6.78%}
         *
         */
    }






    /**
     * 开始录制
     */
    private void startRecord(){
        this.runningMode = DisplayProvider.RECORDING_MODE;
        provider.startRecording();
        binder.provideDisplayView(null, null);
    }

    /**
     * 停止录制
     */
    private void stopRecord(){
        this.runningMode = DisplayProvider.DISPLAY_MODE;
        final Map<RecordPattern, List<RecordPattern.RecordItem>> result = provider.stopRecording();
        binder.provideDisplayView(provideMainView(binder.loadServiceContext()), new LinearLayout.LayoutParams(ContextUtil.dip2px(binder.loadServiceContext(), 280),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        final String uploadUrl = SPService.getString(SPService.KEY_PERFORMANCE_UPLOAD, null);
        BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (StringUtil.isEmpty(uploadUrl)){
                    File folder = RecordUtil.saveToFile(result);
                    //显示提示框
                    MyApplication.getInstance().showDialog(binder.loadServiceContext(),"数据已经保存到\"" + folder.getPath() +"\"下",
                            "确定", null);
                }else {
                    String response = RecordUtil.uploadData(uploadUrl, result);
                    MyApplication.getInstance().showDialog(binder.loadServiceContext(), "录制数据已经上传至\"" + uploadUrl + "\"，" +
                            "响应结果: " + response , "确定", null);
                }
            }
        });

    }

    /**
     * 提供主界面
     * @param context
     * @return
     */
    private View provideMainView(Context context){
        if (runningMode == DisplayProvider.RECORDING_MODE){
            return null;
        }
        floatWinList = (RecyclerView) LayoutInflater.from(context).inflate(R.layout.display_main_layout, null);
        floatWinList.setLayoutManager(new LinearLayoutManager(context));
 /*       floatWinList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });*/
        floatWinAdapter = new FloatWinAdapter(context, this, currentDisplayInfo);
        floatWinList.setAdapter(floatWinAdapter);
        floatWinList.addItemDecoration(new RecycleViewDivider(context, HORIZONTAL_LIST, 0, context.getResources().getColor(R.color.divider_color1)));
        return floatWinList;
    }

    private View provideExpendView(Context context) {
        return null;
    }


    /**
     * 触发显示项
     */
    public void triggerInfo(DisplayItemInfo info){
        if (currentDisplayInfo.contains(info)){
            provider.triggerItem(info.getName());
        }else {
            LogUtil.w(TAG, "显示项【%s】不可用", info);
        }

    }

    private static class DisplayConnection implements ServiceConnection{
        private WeakReference<DisplayManager> ref;

        public DisplayConnection(DisplayManager manager) {
            this.ref = new WeakReference<>(manager);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.d(TAG,"进入onServiceConnected");
            if (ref.get() == null){
                return;
            }
            PerFloatService.PerFloatBinder binder = (PerFloatService.PerFloatBinder)service;
            DisplayManager manager = ref.get();
            Context context = binder.loadServiceContext();
            manager.floatWinAdapter = new FloatWinAdapter(context, manager, manager.currentDisplayInfo);
            //主界面
            binder.provideDisplayView(manager.provideMainView(context), new LinearLayout.LayoutParams(ContextUtil.dip2px(context, 280),
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            //扩展界面
            binder.provideExpendView(manager.provideExpendView(context), null);
            manager.binder = binder;
            binder.registerStopClickListener(manager.stopListener);
            binder.registerRunClickListener(manager.runListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.d(TAG,"进入onServiceDisconnected");
            if (ref.get() == null){
                return;
            }
            //设置悬浮窗界面
            PerFloatService.PerFloatBinder binder = ref.get().binder;
            ref.get().binder = null;
            //隐藏界面
            binder.provideExpendView(null,null);
            binder.provideDisplayView(null,null);
            binder.registerRunClickListener(null);
            binder.registerStopClickListener(null);
            binder.stopFloat();
        }
    }

    private static class MyRunningListener implements PerFloatService.OnRunListener{
        private WeakReference<DisplayManager> manRef;

        public MyRunningListener(DisplayManager manRef) {
            this.manRef = new WeakReference<>(manRef);
        }

        @Override
        public int onRunClick() {
            if (manRef.get() == null){
                LogUtil.d(TAG,"Man被回收");
                return 0;
            }
            //更新显示图标
            DisplayManager manager = manRef.get();
            if (manager.runningMode == DisplayProvider.DISPLAY_MODE){
                manager.startRecord();
                return PerFloatService.RECORDING_ICON;
            }else if (manager.runningMode == DisplayProvider.RECORDING_MODE){
                manager.stopRecord();
                return PerFloatService.PLAY_ICON;
            }
            return 0;
        }
    }







}
