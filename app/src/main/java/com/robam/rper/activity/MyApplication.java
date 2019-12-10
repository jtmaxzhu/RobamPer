package com.robam.rper.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.multidex.MultiDex;
import android.view.WindowManager;
import android.widget.Toast;

import com.liulishuo.filedownloader.FileDownloader;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.CsvFormatStrategy;
import com.orhanobut.logger.DiskLogAdapter;
import com.orhanobut.logger.Logger;
import com.robam.rper.R;
import com.robam.rper.annotation.LocalService;
import com.robam.rper.injector.InjectorService;
import com.robam.rper.injector.param.SubscribeParamEnum;
import com.robam.rper.library.Enhancer;
import com.robam.rper.library.EnhancerInterface;
import com.robam.rper.library.MethodInterceptor;
import com.robam.rper.library.MethodProxy;
import com.robam.rper.logger.DiskLogStrategy;
import com.robam.rper.logger.SimpleFormatStrategy;
import com.robam.rper.service.SPService;
import com.robam.rper.service.base.ExportService;
import com.robam.rper.serviceTest.testdemo;
import com.robam.rper.tools.BackgroundExecutor;
import com.robam.rper.util.ClassUtil;
import com.robam.rper.util.FileUtils;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.RperCrashHandler;
import com.robam.rper.util.StringUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * author : liuxiaohu
 * date   : 2019/8/3 15:54
 * desc   :
 * version: 1.0
 */
public class MyApplication extends Application {

    private final static String TAG = MyApplication.class.getSimpleName();
    private static volatile boolean appInt = false;

    public static final String SHOW_LOADING_DIALOG = "showLoadingDialog";
    public static final String DISMISS_LOADING_DIALOG = "dismissLoadingDialog";


    protected static MyApplication appInstance;
    private Handler handler;
    protected Map<String, ServiceReference> registeredService = new HashMap<>();
    // 主线程借助
    private volatile boolean MAIN_THREAD_WAIT = false;
    private Queue<Runnable> MAIN_THREAD_RUNNABLES = new ConcurrentLinkedQueue<>();

    private InjectorService injectorService;
    private static String curSysInputMethod;

    //已安装应用列表
    private final List<ApplicationInfo> packageList = new ArrayList<>();
    private static final long DAY_MILLIONS = 24 * 60 * 60 * 1000;

    //activity 数据计数器
    private int activityCount = 0;

    private AlertDialog dialog;

    /**
     * 是否是DEBUG
     */
    public static boolean DEBUG = false;

    /**
     * 屏幕方向监控
     */
    public static final String SCREEN_ORIENTATION = "screenOrientation";

    private Stack<ContextInstanceWrapper> openedActivity = new Stack<>();
    private Stack<ContextInstanceWrapper> openedService = new Stack<>();


    public HashMap<String, Object> hashMap = new HashMap<>();


    private WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
    private WindowManager.LayoutParams floatWinParams = new WindowManager.LayoutParams();

    public WindowManager.LayoutParams getMywmParams() {
        return wmParams;
    }

    public WindowManager.LayoutParams getFloatWinParams() {
        return floatWinParams;
    }

    /**
     * 获取实例
     * @return
     */
    public static MyApplication getInstance(){
        return appInstance;
    }


    /**
     * 获得Context
     * @return
     */
    public static Context getContext(){
        return appInstance;
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * 定时任务
     */
    private static TimerTask CLEAR_FILES_TASK = new TimerTask() {
        @Override
        public void run() {
            int clearDays = SPService.getInt(SPService.KEY_AUTO_CLEAR_FILES_DAYS,3);
            if (clearDays < 0) {
                return;
            }
            // 待清理文件夹
            File[] clearFolders = FileUtils.getAutoClearDirs();

            for (File downloadDir: clearFolders) {
                long currentTime = System.currentTimeMillis();
                long clearTime = currentTime - clearDays * DAY_MILLIONS;

                // 如果设置为0，一天就清理
                if (clearDays == 0) {
                    clearTime = currentTime - DAY_MILLIONS;
                }

                File[] subFiles = downloadDir.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        // 删除文件
                        if (subFile.isFile() && subFile.lastModified() <= clearTime) {
                            FileUtils.deleteFile(subFile);
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onCreate() {
        // ClassUtil没有加载过类，说明是第一次启动，或者crash了
        if (!ClassUtil.recordCLasses()) {
            appInt = false;
        }
        //判断是否是调试模式
        ApplicationInfo info = getApplicationInfo();
        DEBUG = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        super.onCreate();
        appInstance = this;
        initialLogger();

        handler = new Handler();
        BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //获取所有类文件
                    ClassUtil.initClasses(MyApplication.this, null);
                    //初始化基础服务
                    registerServices();
                    //初始化
                    init();
                    appInt = true;
                    FileUtils.createFile("Monkeylog");

                }catch (Throwable e){
                    throw new RuntimeException(e);
                }
            }
        });
        // 主线程初始化
        SPService.init(this);
        // 监听应用所有Activity的运行情况
        registerLifecycleCallbacks();

    }

    private void registerLifecycleCallbacks(){
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                activityCount++;
                if (activityCount == 1) {
                   // EventBus.getDefault().post(new AppForegroundEvent(true));
                }
                LogUtil.d(TAG,"activity start："+activity.getLocalClassName());

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                activityCount--;
                if (activityCount == 0) {
                //    EventBus.getDefault().post(new AppForegroundEvent(false));
                }
                LogUtil.d(TAG,"activity stop："+activity.getLocalClassName());
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    protected void registerServices(){
        //获取实现ExportService接口的且有LocalService注解的类
        List<Class<? extends ExportService>> serviceClasses = ClassUtil.findSubClass(ExportService.class, LocalService.class);
        for (Class<? extends ExportService> s:serviceClasses){
            LogUtil.d(TAG,"实现ExportService接口的且有LocalService注解的类:"+s);
        }
        //注册服务
        _registerServices(serviceClasses);

    }

    private void _registerServices(List<Class<? extends ExportService>> serviceClasses){
        if (serviceClasses != null && serviceClasses.size() > 0){
            for (Class<? extends ExportService> childClass : serviceClasses){
                LocalService anno = childClass.getAnnotation(LocalService.class);
                String name = anno.name();
                if(StringUtil.isEmpty(name)){
                    name = childClass.getName();
                }
                //如果注册过，比较level，确定保留哪一个服务
                if (registeredService.containsKey(name)) {
                    ServiceReference prevService = registeredService.get(name);
                    if (anno.level() <= prevService.level) {
                        continue;
                    } else {
                        // 清理掉之前注册的服务
                        prevService.onDestroy(this);
                    }
                }

                ServiceReference reference = new ServiceReference(anno, childClass);
                registeredService.put(name, reference);

            }
        }
    }

    /**
     * 初始化信息
     */
    public void init(){
        //注册自身信息
        injectorService = findServiceByName(InjectorService.class.getName());
        injectorService.register(this);
        //文件下载和crash日志存储
        initLibraries();

        // 后台加载下应用列表
        BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (MyApplication.class) {
                    loadApplicationList();
                }
            }
        });

        Timer timer = new Timer("AUTO_CLEAR_FILE");
        timer.schedule(CLEAR_FILES_TASK, 5*1000, 3 * 60 * 60 * 1000);
        testdemo ts = new testdemo();
        ts.testdemoPro();
    }


    /**
     * 获取应用列表
     * @return
     */
    public List<ApplicationInfo> loadAppList() {
        LogUtil.d(TAG, "in my loadAppList: ");
        synchronized (MyApplication.class) {
            if (packageList.isEmpty()) {
                loadApplicationList();
            }
        }
        LogUtil.d(TAG, "out my loadAppList: ");
        return packageList;
    }

    /**
     * 重新加载应用列表
     */
    private void loadApplicationList() {
        // 后台加载下应用列表
        List<ApplicationInfo> listPack = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        List<ApplicationInfo> removedItems = new ArrayList<>();

        for (ApplicationInfo pack: listPack) {
         /*   if ((pack.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                removedItems.add(pack);
            }*/

            // 移除自身
            if (StringUtil.equals(getPackageName(), pack.packageName)) {
                removedItems.add(pack);
            }
        }
        listPack.removeAll(removedItems);

        // 排序下
        final Comparator c = Collator.getInstance(Locale.CHINA);
        Collections.sort(listPack, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo o1, ApplicationInfo o2) {
                String n1 = o1.loadLabel(getPackageManager()).toString();
                String n2 = o2.loadLabel(getPackageManager()).toString();
                return c.compare(n1, n2);
            }
        });



        packageList.clear();
        packageList.addAll(listPack);
    }

    /**
     * 切换到主线程运行函数
     * @param runnable
     */
    public void runOnUiThread(Runnable runnable){
        if (MAIN_THREAD_WAIT){
            MAIN_THREAD_RUNNABLES.add(runnable);
            return;
        }
        if (Thread.currentThread() != Looper.getMainLooper().getThread()){
            handler.post(runnable);
        }else {
            runnable.run();
        }
    }

    protected void initialLogger() {
        // 非调试模式走CSV Log
        if (!DEBUG) {
            File logDir = new File(getContext().getExternalCacheDir(), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            CsvFormatStrategy strategy = CsvFormatStrategy.newBuilder().tag("RobamPer").logStrategy(new DiskLogStrategy(logDir)).build();
            Logger.addLogAdapter(new DiskLogAdapter(strategy) {
                @Override
                public boolean isLoggable(int priority, String tag) {
                    if (priority < Logger.INFO) {
                        return false;
                    }

                    return true;
            }
            });
        } else {
            // 调试模式走SimpleFormat
            SimpleFormatStrategy formatStrategy = new SimpleFormatStrategy();
            Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy) {
                @Override
                public boolean isLoggable(int priority, String tag) {
                    return true;
                }
            });
        }
    }

    private void initLibraries() {
        LogUtil.d(TAG, "in my initLibraries: ");
//        initGreenDao();
        initFileDownloader();

        curSysInputMethod = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);

        // 兜底记录未捕获的日志
        RperCrashHandler handler = RperCrashHandler.getRperCrashHandler();
        handler.registerCrashCallback(new RperCrashHandler.CrashCallback() {
            @Override
            public void onAppCrash(Thread t, Throwable e) {
                File errorDir = FileUtils.getSubDir("error");
                File outputFile = new File(errorDir, System.currentTimeMillis() + ".log");
                try {
                    FileWriter writer = new FileWriter(outputFile);
                    PrintWriter printWriter = new PrintWriter(writer);
                    printWriter.println("故障线程：" + t.getName());
                    printWriter.println("故障日志：");
                    e.printStackTrace(printWriter);
                    printWriter.flush();
                    printWriter.close();
                } catch (IOException e1) {
                    LogUtil.e(TAG, "Catch java.io.IOException: " + e1.getMessage(), e);
                }
            }
        });
        handler.init();
        LogUtil.d(TAG, "out my initLibraries: ");
    }

    private void initFileDownloader() {
        FileDownloader.setup(this);
    }

    private void initGreenDao() {
   //     GreenDaoManager.getInstance();
    }

    /**
     * 根据名称获取服务
     * @param name
     * @return
     */
    public <T extends ExportService> T findServiceByName(String name) {
        if (registeredService.containsKey(name)) {
            final ServiceReference reference = registeredService.get(name);
            final T target =  (T) reference.getService();
            if (target instanceof EnhancerInterface) {
                Object realTarget = ((EnhancerInterface) target).getTarget$Enhancer$();
                if (realTarget == null) {
                    final CountDownLatch latch = new CountDownLatch(1);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((EnhancerInterface) target).setTarget$Enhancer$(reference.initClass());
                            latch.countDown();
                        }
                    });

                    // 等待对应service初始化完毕
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        LogUtil.e(TAG, "Catch java.lang.InterruptedException: " + e.getMessage(), e);
                    }
                }
            }

            return target;
        }
        return null;
    }

    public void stopServiceByName(String name){
        if (registeredService.containsKey(name)){
            ServiceReference ref = registeredService.get(name);
            ref.onDestroy(getContext());
        }
    }

    /**
     * 主线程等待函数
     * @return
     */
    public boolean waitInMain(){
      while (!appInt) {
          if(Looper.getMainLooper().getThread() == Thread.currentThread()){
                MAIN_THREAD_WAIT = true;
                while(!MAIN_THREAD_RUNNABLES.isEmpty()){
                    Runnable r = MAIN_THREAD_RUNNABLES.poll();
                    LogUtil.d(TAG,"Runnable r:"+r);
                    if(r != null){
                        r.run();
                    }
                }
          }
      }
      MAIN_THREAD_WAIT = false;
      return true;
    }

    /**
     * 通知创建context
     * @param context
     */
    public void notifyCreate(Context context){
        if(context == null){
            return;
        }
        String name = context.getClass().getName();
        if(context instanceof Service){
            ContextInstanceWrapper wrapper = findTargetContext(openedService, context);
            if(wrapper != null){
                LogUtil.d(TAG,"目标服务[%s]已经创建",name);
                return;
            }
            //加service
            SERVICE_STACK_LOCK.writeLock().lock();
            wrapper = new ContextInstanceWrapper(name, context,ContextRunningStatus.CREATE);
            openedService.push(wrapper);
            SERVICE_STACK_LOCK.writeLock().unlock();
        }else if(context instanceof Activity){
            ContextInstanceWrapper wrapper = findTargetContext(openedActivity, context);
            if(wrapper != null){
                LogUtil.d(TAG,"activity create: %s",name);
                return;
            }
            //加service
            ACTIVITY_STACK_LOCK.writeLock().lock();
            wrapper = new ContextInstanceWrapper(name, context,ContextRunningStatus.CREATE);
            openedActivity.push(wrapper);
            ACTIVITY_STACK_LOCK.writeLock().unlock();
        }
    }


    /**
     * 通知context显示
     * @param context
     */
    public void notifyResume(Context context){
        if(context == null){
            return;
        }
        String name = context.getClass().getName();
        if(context instanceof Activity){
            ContextInstanceWrapper wrapper = findTargetContext(openedActivity, context);
            if(wrapper != null){
                wrapper.updateStatus(ContextRunningStatus.RESUME);
                LogUtil.d(TAG,"activity resume: %s",name);
            }else{
                //加service
                ACTIVITY_STACK_LOCK.writeLock().lock();
                wrapper = new ContextInstanceWrapper(name, context,ContextRunningStatus.RESUME);
                openedActivity.push(wrapper);
                ACTIVITY_STACK_LOCK.writeLock().unlock();
                LogUtil.d(TAG,"活动[%s]还没有开始",name);
            }
        }else {
            LogUtil.d(TAG, "Context %s 不能恢复", name);
        }
    }


    /**
     * 通知Context暂停
     * @param context
     */
    public void notifyPause(Context context) {
        if (context == null) {
            return;
        }

        // 先记录下名称
        String name = context.getClass().getName();
        if (context instanceof Activity) {
            // 找下目标Context
            ContextInstanceWrapper target = findTargetContext(openedActivity, context);

            // 找到目标Activity
            if (target != null) {
                target.updateStatus(ContextRunningStatus.PAUSE);
                LogUtil.i(TAG, "activity pause: %s", name);
            } else {
                // 没找到，不操作
                LogUtil.w(TAG, "Activity %s pause without start", name);
            }
        } else {
            LogUtil.e(TAG, "Context %s can't resume", name);
        }
    }

    /**
     * 通知context销毁
     * @param context
     */
    public void notifyDestroy(Context context) {
        if (context == null) {
            return;
        }

        // 先记录下名称
        String name = context.getClass().getName();
        if (context instanceof Activity) {
            // 找下目标Context
            ContextInstanceWrapper target = findTargetContext(openedActivity, context);

            // 找到目标Activity
            if (target != null) {
                target.updateStatus(ContextRunningStatus.DESTROY);
                LogUtil.i(TAG, "activity destroy: %s", name);

                // 清理下Activity
                clearDestroyedContext(openedActivity);
            } else {
                // 没找到，不操作
                LogUtil.w(TAG, "Activity %s destroy without start", name);
            }
        } else if (context instanceof Service) {
            // 找下目标Context
            ContextInstanceWrapper target = findTargetContext(openedService, context);

            // 找到目标Activity
            if (target != null) {
                target.updateStatus(ContextRunningStatus.DESTROY);
                LogUtil.i(TAG, "Update activity %s to destroy state", name);

                // 清理下Service
                clearDestroyedContext(openedService);
            } else {
                // 没找到，不操作
                LogUtil.w(TAG, "Activity %s destroy without start", name);
            }
        } else {
            LogUtil.e(TAG, "Context %s can't resume", name);
        }
    }



    // Activity栈读写锁
    private final ReentrantReadWriteLock ACTIVITY_STACK_LOCK = new ReentrantReadWriteLock();

    // Service栈读写锁
    private final ReentrantReadWriteLock SERVICE_STACK_LOCK = new ReentrantReadWriteLock();


    /**
     *  查找目标Context
     * @param stack
     * @param context
     * @return
     */
    private ContextInstanceWrapper findTargetContext(Stack<ContextInstanceWrapper> stack, Context context){
        if(stack == null || context == null){
            return null;
        }
        ReentrantReadWriteLock.ReadLock readLock = null;
        if(stack == openedActivity){
            readLock = ACTIVITY_STACK_LOCK.readLock();
        }else if(stack == openedService){
            readLock = SERVICE_STACK_LOCK.readLock();
        }
        if(readLock != null){
            readLock.lock();
        }
        for (ContextInstanceWrapper target:stack){
            if(target.isTargetContext(context)){
                if(readLock != null){
                    readLock.unlock();
                }
                return target;
            }
        }
        if (readLock != null){
            readLock.unlock();
        }
        return null;
    }


    /**
     * 清理无用Context
     * @param stack
     */
    public void clearDestroyedContext(Stack<ContextInstanceWrapper> stack) {
        ReentrantReadWriteLock.WriteLock writeLock = null;

        // 查找对应锁
        if (stack == openedActivity) {
            writeLock = ACTIVITY_STACK_LOCK.writeLock();
        } else if (stack == openedService) {
            // 不修改，加读锁
            writeLock = SERVICE_STACK_LOCK.writeLock();
        }

        if (writeLock != null) {
            writeLock.lock();
        }
        // 清理掉Destroy的Context
        Iterator<ContextInstanceWrapper> iterator = stack.iterator();
        while (iterator.hasNext()) {
            ContextInstanceWrapper item = iterator.next();
            if (!item.checkValid()) {
                iterator.remove();
            }
        }

        if (writeLock != null) {
            writeLock.unlock();
        }
    }



    /**
     * 是否初始化完毕
     * @return
     */
    public boolean hasFinishInit() {
        return appInt;
    }

    /**
     * 服务引用
     */
    private static class ServiceReference{
        private Class<? extends ExportService> targetClass;
        private ExportService target;
        private int level;

        public ServiceReference(LocalService annotation, Class<? extends ExportService> targetClass) {
            this.targetClass = targetClass;
            this.level = annotation.level();
            initService(targetClass, annotation.lazy());

        }

        private synchronized ExportService getService(){
            return target;
        }

        private void initService(Class<? extends ExportService> target, boolean lazy){
            LogUtil.d(TAG,"target:"+target+",lazy:"+lazy);
            if (ExportService.class.isAssignableFrom(target)){
                //创建一个增强器，用来在运行时生成类
                Enhancer enhancer = new Enhancer(getContext());
                //设置要继承的目标类
                enhancer.setSuperclass(target);
                //设置MethodInterceptor
                enhancer.setCallback(new MethodInterceptor() {
                    @Override
                    public Object intercept(Object object, Object[] args, MethodProxy methodProxy) throws Exception {
                        LogUtil.d(TAG, "当前类型：%s", object.getClass());
                        final EnhancerInterface enhancerInterface = (EnhancerInterface) object;
                        LogUtil.d(TAG, "convert类型：%s", enhancerInterface.getClass());
                        ExportService target = (ExportService) enhancerInterface.getTarget$Enhancer$();
                        LogUtil.d(TAG, "target类型:%s", target);
                        if (target == null) {
                            final AtomicBoolean runningFLag = new AtomicBoolean(true);

                            // 在主线程运行
                            getInstance().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    enhancerInterface.setTarget$Enhancer$(initClass());
                                    runningFLag.set(false);
                                }
                            });

                            long startTime = System.currentTimeMillis();
                            // 等待加载完毕，最长3s
                            while (System.currentTimeMillis() - startTime < 3000) {
                                if (!runningFLag.get()) {
                                    break;
                                }
                                try {
                                    Thread.sleep(2);
                                } catch (InterruptedException e) {
                                    LogUtil.e(TAG, "Catch java.lang.InterruptedException: " + e.getMessage(), e);
                                }
                            }

                            target = (ExportService) enhancerInterface.getTarget$Enhancer$();
                        }
                        return methodProxy.invokeSuper(target, args);
                    }
                });
                //生成新的代理类
                final EnhancerInterface result = (EnhancerInterface) enhancer.create();

                // 非lazy模式
                if (!lazy) {
                    getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ExportService service = initClass();
                            result.setTarget$Enhancer$(service);
                        }
                    });
                }

                this.target = (ExportService) result;
            }
        }

        /**
         * 构造
         * @return
         */
        private ExportService initClass() {
            ExportService target = ClassUtil.constructClass(targetClass);
            if (target == null) {
                LogUtil.e(TAG, "初始化类失败，className=%s", targetClass);
                return null;
            }
            target.onCreate(getContext());
            return target;
        }

        /**
         * 调用清理
         * @param context
         */
        private void onDestroy(final Context context) {
            EnhancerInterface target = (EnhancerInterface) this.target;
            final ExportService service = (ExportService) target.getTarget$Enhancer$();
            if (service != null) {
                getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        service.onDestroy(context);
                    }
                });
                // 设置为空
                target.setTarget$Enhancer$(null);
            }
        }

    }

    /**
     * Context运行状态
     */
    private enum ContextRunningStatus {
        CREATE,
        RESUME,
        PAUSE,
        DESTROY
    }

    /**
     * Context维护
     */
    private static class ContextInstanceWrapper{
        private String name;
        private WeakReference<Context> currentContext;
        private ContextRunningStatus status;

        public ContextInstanceWrapper(String name, Context currentContext, ContextRunningStatus status) {
            this.name = name;
            this.currentContext = new WeakReference<>(currentContext);
            this.status = status;
        }

        /**
         * 更新Context状态
         *
         * @param status
         */
        public void updateStatus(ContextRunningStatus status) {
            this.status = status;
        }

        /**
         * 生命周期校验
         *
         * @return
         */
        public boolean checkValid() {
            return status != ContextRunningStatus.DESTROY && this.currentContext.get() != null;
        }

        /**
         * 检测是否是目标Context
         *
         * @param context
         * @return
         */
        public boolean isTargetContext(Context context) {
            return context != null && context == this.currentContext.get();
        }

        /**
         * 是否正在运行
         * @return
         */
        public boolean isRunning() {
            return currentContext.get() != null &&
                    (status == ContextRunningStatus.CREATE ||
                            status == ContextRunningStatus.RESUME);
        }
    }

    /**
     * 展示提示框
     * @param message
     * @param positiveText
     * @param positiveRunnable
     */
    public void showDialog(Context context, final String message, final String positiveText,
                           final Runnable positiveRunnable) {
        showDialog(context, message, positiveText, positiveRunnable, null, null);
    }


    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == AlertDialog.BUTTON_POSITIVE) {
                if (positiveRunnable != null) {
                    positiveRunnable.run();
                }
            } else if (which == AlertDialog.BUTTON_NEGATIVE){
                if (negativeRunnable != null) {
                    negativeRunnable.run();
                }
            }

            dialog.dismiss();
        }
    };


    private Runnable positiveRunnable = null;

    private Runnable negativeRunnable = null;

    private static int WINDOW_TYPE = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
    /**
     * 展示加载框
     *
     * @param message
     */
    public void showDialog(final Context context, final String message, final String positiveText,
                           final Runnable positiveRunnable, final String negativeText,
                           final Runnable negativeRunnable) {
        this.positiveRunnable = positiveRunnable;
        this.negativeRunnable = negativeRunnable;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.PermissionAppDialogTheme)
                            .setMessage(message)
                            .setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (positiveRunnable != null) {
                                        positiveRunnable.run();
                                    }
                                    dialog.dismiss();
                                }
                            });
                    if (!StringUtil.isEmpty(negativeText)) {
                        builder.setNegativeButton(negativeText, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (negativeRunnable != null) {
                                    negativeRunnable.run();
                                }
                                dialog.dismiss();
                            }
                        });
                    }
                    dialog = builder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.setCanceledOnTouchOutside(false);                                   //点击外面区域不会让dialog消失
                    dialog.setCancelable(false);
                } else {
                    dialog.setMessage(message);
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, positiveText, listener);
                    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, negativeText, listener);
                }

                try {
                    dialog.show();
                } catch (WindowManager.BadTokenException e) {
                    LogUtil.e(TAG, "Unable to show with TYPE_SYSTEM_ALERT", e);
                    WINDOW_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
                }
            }
        });
    }

    /**
     * 获取当前屏幕显示的Activity
     * @return
     */
    public Context loadActivityOnTop() {
        // 找Activity
        ACTIVITY_STACK_LOCK.readLock().lock();
        for (ContextInstanceWrapper wrapper : openedActivity) {
            if (wrapper.isRunning()) {
                ACTIVITY_STACK_LOCK.readLock().unlock();
                return wrapper.currentContext.get();
            }
        }

        ACTIVITY_STACK_LOCK.readLock().unlock();
        // 没找到，返回空
        return null;
    }

    /**
     * 获取当前屏幕显示的Service
     * @return
     */
    public Context loadRunningService() {
        // 找Service
        SERVICE_STACK_LOCK.readLock().lock();
        for (ContextInstanceWrapper wrapper : openedService) {
            if (wrapper.isRunning()) {
                SERVICE_STACK_LOCK.readLock().unlock();
                return wrapper.currentContext.get();
            }
        }

        SERVICE_STACK_LOCK.readLock().unlock();
        // 没找到，返回空
        return null;
    }

    /**
     * 显示Toast
     * @param message
     */
    public void showToast(String message) {
        showToast(getContext(), message);
    }

    /**
     * 显示toast
     * @param context
     * @param message
     */
    public void showToast(final Context context, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }



    private String appPackage = null;

    private String appName = null;

    /**
     * 更新调试应用与包名
     *
     * @param appPackage
     * @param appName
     */
    public void updateAppAndName(String appPackage, String appName) {
        this.appName = appName;
        this.appPackage = appPackage;

        // 主动推消息
        injectorService.pushMessage(SubscribeParamEnum.APP, appPackage, true);
        injectorService.pushMessage(SubscribeParamEnum.APP_NAME, appName, true);

       // getSharedPreferences("MonkeyFloatService", MODE_PRIVATE).edit().putString("float_app", appName + "##" + appPackage).apply();
        getSharedPreferences("PerFloatService", MODE_PRIVATE).edit().putString("float_app", appName + "##" + appPackage).apply();
    }


}
