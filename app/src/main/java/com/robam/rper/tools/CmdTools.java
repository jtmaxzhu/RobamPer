package com.robam.rper.tools;

import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.util.Base64;

import com.robam.adblib.AdbBase64;
import com.robam.adblib.AdbConnection;
import com.robam.adblib.AdbCrypto;
import com.robam.adblib.AdbStream;
import com.robam.rper.activity.MyApplication;
import com.robam.rper.injector.InjectorService;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.MiscUtil;
import com.robam.rper.util.StringUtil;
import com.robam.rper.util.rom.RomUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * author : liuxiaohu
 * date   : 2019/10/15 9:43
 * desc   :
 * version: 1.0
 */
public class CmdTools {
    private static String TAG = "CmdTools";
    public static final String FATAL_ADB_CANNOT_RECOVER = "fatalAdbNotRecover";
    private static ExecutorService cachedExecutor = Executors.newCachedThreadPool();

    private static volatile AdbConnection connection;


    public static AdbBase64 getBase64Impl() {
        return new AdbBase64() {
            @Override
            public String encodeToString(byte[] arg0) {
                return Base64.encodeToString(arg0, 2);
            }
        };
    }

    /**
     * 是否已初始化
     * @return
     */
    public static boolean isInitialized() {
        return connection != null;
    }

    public static synchronized boolean generateConnection(){
        if(connection != null && connection.isFine()){
            return true;
        }
        if (connection != null){
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                connection = null;
            }
        }
        Socket sock;
        AdbCrypto crypto;
        AdbBase64 base64 = getBase64Impl();

        // 获取连接公私钥
        File privKey = new File(MyApplication.getInstance().getFilesDir(), "privKey");
        File pubKey = new File(MyApplication.getInstance().getFilesDir(), "pubKey");

        if (!privKey.exists() || !pubKey.exists()) {
            try {
                // 通过生成一个新的密钥对来创建一个新的AdbCrypto对象
                crypto = AdbCrypto.generateAdbKeyPair(base64);
                privKey.delete();
                pubKey.delete();
                crypto.saveAdbKeyPair(privKey, pubKey);
            } catch (NoSuchAlgorithmException | IOException e) {
                return false;
            }
        } else {
            try {
                crypto = AdbCrypto.loadAdbKeyPair(base64, privKey, pubKey);
            } catch (Exception e) {
                try {
                    crypto = AdbCrypto.generateAdbKeyPair(base64);
                    privKey.delete();
                    pubKey.delete();
                    crypto.saveAdbKeyPair(privKey, pubKey);
                } catch (NoSuchAlgorithmException | IOException ex) {
                    return false;
                }
            }
        }

        // 开始连接adb
        LogUtil.d(TAG, "Socket connecting...");
        try {
            sock = new Socket("localhost", 5555);
        } catch (IOException e) {
            LogUtil.e(TAG, "Throw IOException", e);
            return false;
        }
        LogUtil.i(TAG, "Socket connected");

        //构造AdbConnection对象
        AdbConnection conn;
        try {
            conn = AdbConnection.create(sock, crypto);
            LogUtil.i(TAG, "ADB connecting...");

            // 10s超时
            conn.connect(10 * 1000);
        } catch (Exception e) {
            LogUtil.e(TAG, "ADB connect failed", e);
            // socket关闭
            if (sock.isConnected()) {
                try {
                    sock.close();
                } catch (IOException e1) {
                    LogUtil.e(TAG, "Catch java.io.IOException: " + e1.getMessage(), e);
                }
            }
            return false;
        }
        connection = conn;
        LogUtil.i(TAG, "ADB connected");

        // ADB成功连接后，开启ADB状态监测
        startAdbStatusCheck();
        return true;
    }


    private static ScheduledExecutorService scheduledExecutorService;
    private static volatile long LAST_RUNNING_TIME = 0;
    /**
     * 开始检查ADB状态
     */
    private static void startAdbStatusCheck(){
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                // 防止重复运行，14s内只能执行一次
                if (currentTime - LAST_RUNNING_TIME < 14 * 1000) {
                    return;
                }

                LAST_RUNNING_TIME = currentTime;
                String result = null;
                try {
                    result = execAdbCmd("echo '1'", 5000);
                } catch (Exception e) {
                    LogUtil.e(TAG, "Check adb status throw :" + e.getMessage(), e);
                }

                if (!StringUtil.equals("1", StringUtil.trim(result))) {
                    // 等2s再检验一次
                    MiscUtil.sleep(2000);

                    boolean genResult = false;

                    // double check机制，防止单次偶然失败带来重连
                    String doubleCheck = null;
                    try {
                        doubleCheck = execAdbCmd("echo '1'", 5000);
                    } catch (Exception e) {
                        LogUtil.e(TAG, "Check adb status throw :" + e.getMessage(), e);
                    }
                    if (!StringUtil.equals("1", StringUtil.trim(doubleCheck))) {
                        // 尝试恢复3次
                        for (int i = 0; i < 3; i++) {
                            // 关停无用连接
                            if (connection != null && connection.isFine()) {
                                try {
                                    connection.close();
                                } catch (IOException e) {
                                    LogUtil.e(TAG, "Catch java.io.IOException: " + e.getMessage(), e);
                                } finally {
                                    connection = null;
                                }
                            }

                            // 清理下当前已连接进程
                            clearProcesses();

                            // 尝试重连
                            genResult = generateConnection();
                            if (genResult) {
                                break;
                            }
                        }
                    }

                    // 恢复失败
                    if (!genResult) {
                        Context con = MyApplication.getInstance().loadActivityOnTop();
                        if (con == null) {
                            con = MyApplication.getInstance().loadRunningService();
                        }

                        if (con == null) {
                            MyApplication.getInstance().showToast("ADB连接中断，请尝试重新开启调试端口");
                            return;
                        }

                        // 回首页
                        MyApplication.getInstance().showDialog(con, "ADB连接中断，请尝试重新开启调试端口", "好的", null);

                        // 通知各个功能ADB挂了
                        InjectorService.g().pushMessage(FATAL_ADB_CANNOT_RECOVER);

                        return;
                    }
                }

                // 15S 检查一次
                scheduledExecutorService.schedule(this, 15, TimeUnit.SECONDS);
            }
        }, 15, TimeUnit.SECONDS);
    }

    private static List<AdbStream> streams = new ArrayList<>();
    private static List<Process> processes = new ArrayList<>();

    public static void clearProcesses() {
        try {
            for (Process p : processes) {
                LogUtil.i(TAG, "stop process: " + p.toString());
                p.destroy();
            }
            processes.clear();
            for (AdbStream stream : streams) {
                LogUtil.i(TAG, "stop stream: " + stream.toString());
                try {
                    stream.close();
                } catch (Exception e) {
                    LogUtil.e(TAG, "Stop stream " + stream.toString() + " failed", e);
                }
            }
            streams.clear();
        } catch (Exception e) {
            LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
        }
    }

    /**
     * 执行Adb命令，对外<br/>
     * <b>注意：主线程执行的话超时时间会强制设置为5S以内，防止ANR</b>
     * @param cmd 对应命令
     * @param wait 等待执行时间，0表示一直等待
     * @return 命令行输出
     */
    public static String execAdbCmd(final String cmd, int wait) {
        // 主线程的话走Callable
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (wait > 5000 || wait == 0) {
                LogUtil.w(TAG, "主线程配置的等待时间[%dms]过长，修改为5000ms", wait);
                wait = 5000;
            }

            final int finalWait = wait;
            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() {
                    return _execAdbCmd(cmd, finalWait);
                }
            };
            Future<String> result = cachedExecutor.submit(callable);

            // 等待执行完毕
            try {
                return result.get();
            } catch (InterruptedException e) {
                LogUtil.e(TAG, "Catch java.lang.InterruptedException: " + e.getMessage(), e);
            } catch (ExecutionException e) {
                LogUtil.e(TAG, "Catch java.util.concurrent.ExecutionException: " + e.getMessage(), e);
            }
            return null;
        }
        return _execAdbCmd(cmd, wait);
    }

    /**
     * 执行Adb命令
     * @param cmd 对应命令
     * @param wait 等待执行时间，0表示一直等待
     * @return 命令行输出
     */
    public static String _execAdbCmd(final String cmd, final int wait) {
        if (connection == null) {
            LogUtil.e(TAG, "no connection when execAdbCmd");
            return "";
        }

        try {
            AdbStream stream = connection.open("shell:" + cmd);
            logcatCmd(stream.getLocalId() + "@" + "shell:" + cmd);
            streams.add(stream);

            // 当wait为0，每个10ms观察一次stream状况，直到shutdown
            if (wait == 0) {
                while (!stream.isClosed()) {
                    Thread.sleep(10);
                }
            } else {
                // 等待最长wait毫秒后强制退出
                long start = System.currentTimeMillis();
                while (!stream.isClosed() && System.currentTimeMillis() - start < wait) {
                    Thread.sleep(10);
                }

                if (!stream.isClosed()) {
                    stream.close();
                }
            }

            // 获取stream所有输出
            Queue<byte[]> results = stream.getReadQueue();
            StringBuilder sb = new StringBuilder();
            for (byte[] bytes: results) {
                if (bytes != null) {
                    sb.append(new String(bytes));
                }
            }
            streams.remove(stream);
            return sb.toString();
        } catch (IllegalStateException e) {
            LogUtil.e(TAG, "Throw IllegalStateException: " + e.getMessage(), e);

            LogUtil.e(TAG, "illegal", e);

            if (connection != null) {
                connection.setFine(false);
            }
            boolean result = generateConnection();
            if (result) {
                return retryExecAdb(cmd, wait);
            } else {
                LogUtil.e(TAG, "regenerateConnection failed");
                return "";
            }
        } catch (Exception e){
            LogUtil.e(TAG, "Throw Exception: " + e.getMessage()
                    , e);
            return "";
        }
    }

    protected static void logcatCmd(String cmd){
        LogUtil.i("ADB CMD", cmd);
    }

    private static String retryExecAdb(String cmd, long wait) {
        AdbStream stream = null;
        try {
            stream = connection.open("shell:" + cmd);
            logcatCmd(stream.getLocalId() + "@shell:" + cmd);
            streams.add(stream);

            // 当wait为0，每个10ms观察一次stream状况，直到shutdown
            if (wait == 0) {
                while (!stream.isClosed()) {
                    Thread.sleep(10);
                }
            } else {
                // 等待wait毫秒后强制退出
                long start = System.currentTimeMillis();
                while (!stream.isClosed() && System.currentTimeMillis() - start < wait) {
                    Thread.sleep(10);
                }
                if (!stream.isClosed()) {
                    stream.close();
                }
            }

            // 获取stream所有输出
            Queue<byte[]> results = stream.getReadQueue();
            StringBuilder sb = new StringBuilder();
            for (byte[] bytes: results) {
                if (bytes != null) {
                    sb.append(new String(bytes));
                }
            }
            streams.remove(stream);
            return sb.toString();
        } catch (IOException e) {
            LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
        } catch (InterruptedException e) {
            LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
        }

        return "";
    }

    public static Process getRootCmd(){
        try{
            return Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            LogUtil.e(TAG, "get root shell failed", e);
            isRoot = false;
        }
        return null;
    }

    private static Boolean isRoot = null;
    /**
     * 判断当前手机是否有ROOT权限
     * @returnz
     */
    public static boolean isRooted(){
        boolean bool = false;

        // 避免重复查找文件
        if (isRoot != null) {
            return isRoot;
        }
        try{
            if (new File("/system/bin/su").exists()){
                bool = true;
            } else if (new File("/system/xbin/su").exists()) {
                bool = true;
            } else if (new File("/su/bin/su").exists()) {
                bool = true;
            }
            LogUtil.d(TAG, "isRooted = " + bool);

        } catch (Exception e) {
            LogUtil.e(TAG, "THrow exception: " + e.getMessage(), e);
        }
        return bool;
    }


    public static StringBuilder execCmd(String cmd) {
        InputStreamReader isr = null;
        BufferedReader br = null;
        Process p = null;
        StringBuilder ret = new StringBuilder();
        String line = "";

        try {
            p = Runtime.getRuntime().exec(cmd);// 经过Root处理的android系统即有su命令
            isr = new InputStreamReader(p.getInputStream());

            br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
//            		LogUtil.d(TAG, "ERR************" + line);
                ret.append(line).append("\n");
            }
            br.close();
            p.waitFor();
        } catch (Exception e) {
            LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
            return ret;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {

                    LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
                }
            }
            if(p != null) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
                }
            }
        }
        return ret;
    }

    /**
     * 执行root命令
     * @param cmd 待执行命令
     * @param log 日志输出文件
     * @param ret 是否保留命令行输出
     * @param ct 上下文
     * @return 输出
     */
    @SuppressWarnings("deprecation")
    public static StringBuilder execRootCmd(String cmd, String log, Boolean ret, Context ct) {
        StringBuilder result = new StringBuilder();
        DataOutputStream dos = null;
        DataInputStream dis = null;
        DataInputStream des = null;
        String line = null;
        Process p;

        try {
            p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
            processes.add(p);
            dos = new DataOutputStream(p.getOutputStream());
            dis = new DataInputStream(p.getInputStream());
            des = new DataInputStream(p.getErrorStream());

//            while ((line = des.readLine()) != null) {
//            		LogUtil.d(TAG, "ERR************" + line);
//            }

            LogUtil.i(TAG, cmd);
            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();

            while ((line = dis.readLine()) != null) {
                if(log != null) {
                    writeFileData(log, line, ct);
                }
                if(ret) {
                    result.append(line).append("\n");
                }
            }
            p.waitFor();
            processes.remove(p);
            isRoot = true;
        } catch (Exception e) {
            LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
            isRoot = false;
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
                }
            }
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
                }
            }
        }
        return result;
    }


    private static final int MODE_APPEND = 0;

    public static void writeFileData(String monkeyLog, String message, Context ct) {
        String time = "";
        try {
            FileOutputStream fout = ct.openFileOutput(monkeyLog, MODE_APPEND);

            SimpleDateFormat formatter = new SimpleDateFormat("+++   HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            time = formatter.format(curDate);

            byte [] bytes = message.getBytes();
            fout.write(bytes);
            bytes = (time + "\n").getBytes();
            fout.write(bytes);
            fout.close();
        }
        catch(Exception e) {
            LogUtil.e(TAG, "抛出异常 " + e.getMessage(), e);
        }


    }


    /**
     * 快捷执行ps指令
     * @param filter grep 过滤条件
     * @return 分行结果
     */
    public static String[] ps(String filter) {
        if (!RomUtils.isOppoSystem() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            try {
                Process p;
                if (filter != null && filter.length() > 0) {
                    p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps | grep \"" + filter + "\""});
                } else {
                    p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps"});
                }
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                List<String> results = new ArrayList<>();
                while ((line = br.readLine()) != null) {
//            		LogUtil.d(TAG, "ERR************" + line);
                    results.add(line);
                }
                return results.toArray(new String[results.size()]);
            } catch (IOException e) {
                LogUtil.e(TAG, "Read ps content failed", e);
                return new String[0];
            }
        } else if (Build.VERSION.SDK_INT <= 25) {

            // Android 7.0, 7.1无法通过应用权限获取所有进程
            if (isRooted()) {
                if (filter != null && filter.length() > 0) {
                    return execRootCmd("ps | grep \"" + filter + "\"", null, true, null).toString().split("\n");
                } else {
                    return execRootCmd("ps", null, true, null).toString().split("\n");
                }
            } else {
                if (filter != null && filter.length() > 0) {

                    // 存在ps命令调用超时情况
                    return execAdbCmd("ps | grep \"" + filter + "\"", 2500).split("\n");
                } else {
                    return execAdbCmd("ps", 2500).split("\n");
                }
            }
        } else {
            // Android O ps为toybox实现，功能与标准ps命令基本相同，需要-A参数获取全部进程
            if (isRooted()) {
                if (filter != null && filter.length() > 0) {
                    return execRootCmd("ps -A | grep \"" + filter + "\"", null, true, null).toString().split("\n");
                } else {
                    return execRootCmd("ps -A", null, true, null).toString().split("\n");
                }
            } else {
                if (filter != null && filter.length() > 0) {

                    // 存在ps命令调用超时情况
                    return execAdbCmd("ps -A | grep \"" + filter + "\"", 2500).split("\n");
                } else {
                    return execAdbCmd("ps -A", 2500).split("\n");
                }
            }
        }
    }

}
