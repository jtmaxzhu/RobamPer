package com.robam.rper.service;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
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
import com.robam.rper.display.DisplayItemInfo;
import com.robam.rper.display.DisplayProvider;
import com.robam.rper.display.items.base.RecordPattern;
import com.robam.rper.tools.BackgroundExecutor;
import com.robam.rper.ui.RecycleViewDivider;
import com.robam.rper.util.ContextUtil;
import com.robam.rper.util.LogUtil;
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



    private void stop(){
        runningFlag = false;
        //停止服务
        MyApplication.getInstance().stopServiceByName(DisplayProvider.class.getName());
        if (connection != null){
            MyApplication.getContext().unbindService(connection);
            connection = null;
            binder = null;
        }
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
                    File folder;


                }

            }
        });

    }

    /**
     * 提供主界面
     * @param context
     * @return
     */

    @SuppressLint("ClickableViewAccessibility")
    private View provideMainView(Context context){
        if (runningMode == DisplayProvider.RECORDING_MODE){
            return null;
        }
        floatWinList = (RecyclerView) LayoutInflater.from(context).inflate(R.layout.display_main_layout, null);
        floatWinList.setLayoutManager(new LinearLayoutManager(context));
        floatWinList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        floatWinAdapter = new FloatWinAdapter(context, this, currentDisplayInfo);
        floatWinList.setAdapter(floatWinAdapter);
        floatWinList.addItemDecoration(new RecycleViewDivider(context, HORIZONTAL_LIST, 1, context.getResources().getColor(R.color.divider_color)));
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

            }
            return 0;
        }
    }







}
