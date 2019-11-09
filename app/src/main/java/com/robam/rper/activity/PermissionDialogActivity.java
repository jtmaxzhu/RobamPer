package com.robam.rper.activity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.robam.rper.R;
import com.robam.rper.injector.InjectorService;
import com.robam.rper.permission.FloatWindowManager;
import com.robam.rper.service.SPService;
import com.robam.rper.tools.BackgroundExecutor;
import com.robam.rper.tools.CmdTools;
import com.robam.rper.util.ContextUtil;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.PermissionUtil;
import com.robam.rper.util.StringUtil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PermissionDialogActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "PermissionDialog";
    public static final String PERMISSIONS_KEY = "permissions";
    public static final String PERMISSION_IDX_KEY = "permissionIdx";
    private static final String PERMISSION_SKIP_RECORD = "skipRecord";
    private static final String PERMISSION_GRANT_RECORD = "grantRecord";
    private static final String PERMISSION_GRANT_ADB = "grantAdb";

    public static final int PERMISSION_FLOAT = 1;
    public static final int PERMISSION_ADB = 2;
    public static final int PERMISSION_ROOT = 3;
    public static final int PERMISSION_TOAST = 4;
    public static final int PERMISSION_ACCESSIBILITY = 5;
    public static final int PERMISSION_USAGE = 6;
    public static final int PERMISSION_RECORD = 7;
    public static final int PERMISSION_ANDROID = 8;
    public static final int PERMISSION_DYNAMIC = 9;

    public static volatile boolean runningStatus = false;

    private InjectorService injectorService;

    private TextView permissionPassed;
    private TextView permissionTotal;

    private ProgressBar progressBar;
    private TextView permissionText;

    private LinearLayout actionLayout;
    private LinearLayout positiveButton;
    private TextView positiveBtnText;
    private LinearLayout negativeButton;
    private TextView negativeBtnText;
    private int currentIdx;
    private int totalIdx;

    private int USAGE_REQUEST = 10001;
    private int ACCESSIBILITY_REQUEST = 10002;
    private int M_PERMISSION_REQUEST = 10003;
    private int MEDIA_PROJECTION_REQUEST = 10004;

    private List<GroupPermission> allPermissions;
    private int currentPermissionIdx;

    /**
     * 权限名称映射表
     */
    public static final Map<String, String> PERMISSION_NAMES = new HashMap<String, String>() {
        {
            put(Manifest.permission.READ_CALENDAR, "读取日历");
            put(Manifest.permission.WRITE_CALENDAR, "写入日历");
            put(Manifest.permission.CAMERA, "相机");
            put(Manifest.permission.READ_CONTACTS, "读取联系人");
            put(Manifest.permission.WRITE_CONTACTS, "写入联系人");
            put(Manifest.permission.GET_ACCOUNTS, "获取账户");
            put(Manifest.permission.ACCESS_FINE_LOCATION, "获取精确定位");
            put(Manifest.permission.ACCESS_COARSE_LOCATION, "获取粗略定位");
            put(Manifest.permission.RECORD_AUDIO, "录音");
            put(Manifest.permission.READ_PHONE_STATE, "读取电话状态");
            put(Manifest.permission.CALL_PHONE, "拨打电话");
            put(Manifest.permission.READ_CALL_LOG, "读取通话记录");
            put(Manifest.permission.WRITE_CALL_LOG, "写入通话记录");
            put(Manifest.permission.ADD_VOICEMAIL, "添加语音邮箱");
            put(Manifest.permission.USE_SIP, "使用SIP");
            put(Manifest.permission.BODY_SENSORS, "获取传感器数据");
            put(Manifest.permission.SEND_SMS, "发送短信");
            put(Manifest.permission.RECEIVE_SMS, "接收短信");
            put(Manifest.permission.READ_SMS, "获取短信信息");
            put(Manifest.permission.RECEIVE_WAP_PUSH, "接收Wap Push");
            put(Manifest.permission.RECEIVE_MMS, "接收MMS");
            put(Manifest.permission.READ_EXTERNAL_STORAGE, "读取外部存储");
            put(Manifest.permission.WRITE_EXTERNAL_STORAGE, "写入外部存储");

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_permission_dialog);
        setupWindow();
        initView();
        initControl();
    }

    /**
     * 设置窗体信息
     */
    @SuppressWarnings("deprecation")
    private void setupWindow() {
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = display.getWidth() - ContextUtil.dip2px(this, 48);
        getWindow().setGravity(Gravity.BOTTOM);
    }

    /**
     * 加载界面
     */
    private void initView() {
        permissionPassed = (TextView) findViewById(R.id.permission_success);
        permissionTotal = (TextView) findViewById(R.id.permission_all);

        progressBar = (ProgressBar) findViewById(R.id.permission_loading_progress);
        permissionText = (TextView) findViewById(R.id.permission_text);

        actionLayout = (LinearLayout) findViewById(R.id.permission_action_layout);
        positiveButton = (LinearLayout) findViewById(R.id.permission_positive_button);
        positiveBtnText = (TextView) positiveButton.getChildAt(0);
        negativeButton = (LinearLayout) findViewById(R.id.permission_negative_button);
        negativeBtnText = (TextView) negativeButton.getChildAt(0);
    }

    private void initControl() {
        positiveButton.setOnClickListener(this);
        negativeButton.setOnClickListener(this);

        currentPermissionIdx = getIntent().getIntExtra(PERMISSION_IDX_KEY, -1);
        groupPermissions();
        processPermission();
    }

    @Override
    public void onBackPressed() {
        finish();
        PermissionUtil.onPermissionResult(currentPermissionIdx, false, "取消授权");
    }

    @Override
    protected void onStart() {
        super.onStart();
        runningStatus = true;
    }

    @Override
    public void finish() {
        // 都是手工调finish结束的，所以通过finish判断
        runningStatus = false;
        LogUtil.i(TAG, "权限弹窗Stop");
        super.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LogUtil.d(TAG,"reqCode:"+requestCode);
        if (requestCode == M_PERMISSION_REQUEST) {
            for (int i = 0; i < grantResults.length; i++) {
                int result = grantResults[i];
                if (result != PackageManager.PERMISSION_GRANTED) {
                    LogUtil.i(TAG, "用户不授权%s权限", permissions[i]);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MyApplication.getInstance(), "用户取消授权", Toast.LENGTH_LONG).show();
                        }
                    });

                    // 重新去检查权限
                    processSinglePermission();
                    return;
                }
            }

            processedAction();
        }
    }

    /**
     * 显示操作框
     * @param message 显示文案
     * @param positiveText 确定文案
     * @param positiveAction 确定动作
     */
    private void showAction(String message, String positiveText, Runnable positiveAction) {
        showAction(message, positiveText, positiveAction, null, null);
    }

    /**
     * 显示操作框
     * @param message 显示文案
     * @param positiveText 确定文案
     * @param positiveAct 确定动作
     * @param negativeText 取消文案
     * @param negativeAct 取消动作
     */
    private void showAction(final String message, final String positiveText, final Runnable positiveAct,
                            final String negativeText, final Runnable negativeAct) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                positiveAction = positiveAct;
                negativeAction = negativeAct;

                progressBar.setVisibility(View.GONE);
                actionLayout.setVisibility(View.VISIBLE);

                // 显示文字
                permissionText.setText(Html.fromHtml(StringUtil.patternReplace(message, "\n", "<br/>")));

                // 设置按钮文本
                positiveBtnText.setText(positiveText);
                // 如果取消非空
                if (!StringUtil.isEmpty(negativeText)) {
                    negativeButton.setVisibility(View.VISIBLE);
                    negativeBtnText.setText(negativeText);
                } else {
                    negativeButton.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * 开始处理权限
     */
    public void processPermission() {
        if (allPermissions == null || allPermissions.size() == 0) {
            showAction(StringUtil.getString(R.string.permission_list_error), "确定", new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }

        currentIdx = -1;
        totalIdx = allPermissions.size();

        // 设置待处理总数
        permissionTotal.setText(StringUtil.toString(totalIdx));

        // 开始处理权限
        processedAction();
    }

    /**
     * 当前权限处理
     */
    private void processedAction() {
        LogUtil.d(TAG,"processedAction() in" );
        currentIdx++;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                actionLayout.setVisibility(View.GONE);
                permissionPassed.setText(StringUtil.toString(currentIdx + 1));

                if (currentIdx >= totalIdx) {
                    finish();
                    PermissionUtil.onPermissionResult(currentPermissionIdx, true, null);
                    return;
                }

                // 开始处理下一条权限
               processSinglePermission();
            }
        });
        LogUtil.d(TAG,"processedAction() out" );
    }


    /**
     * 处理单项权限
     */
    private void processSinglePermission() {
        LogUtil.d(TAG,"processSinglePermission() in" );

        final GroupPermission permission = allPermissions.get(currentIdx);
        // 按照权限组别处理
        switch (permission.permissionType) {
            case PERMISSION_ADB:
                if (!processAdbPermission()) {
                    return;
                }
                break;
            case PERMISSION_FLOAT:
                if(!processFloatPermission()) {
                    return;
                }
                break;
            case PERMISSION_DYNAMIC:
                if (!processDynamicPermission(permission)) {
                    return;
                }
                break;
        }

        processedAction();

        LogUtil.d(TAG,"processSinglePermission() out" );
    }


    /**
     * 处理ADB权限
     * @return
     */
    private boolean processAdbPermission() {
        BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean status;
                if (SPService.getBoolean(PERMISSION_GRANT_ADB, false)){
                    status = CmdTools.generateConnection();
                }else {
                    status = CmdTools.isInitialized();
                }
                if (!status){
                    showAction(StringUtil.getString(R.string.adb_permission), "确定",
                            new Runnable() {
                                @Override
                                public void run() {
                                    SPService.putBoolean(PERMISSION_GRANT_ADB, true);
                                    progressBar.setVisibility(View.VISIBLE);
                                    permissionText.setText(R.string.adb_open_advice);
                                    positiveButton.setEnabled(false);
                                    BackgroundExecutor.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            boolean result;
                                            result = CmdTools.generateConnection();
                                            if (result) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        positiveButton.setEnabled(true);
                                                    }
                                                });
                                                processedAction();
                                            } else {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        progressBar.setVisibility(View.GONE);
                                                        permissionText.setText(R.string.open_adb_permission_failed);
                                                        positiveButton.setEnabled(true);
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }, "取消", new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                    PermissionUtil.onPermissionResult(currentPermissionIdx, false, "ADB连接失败");
                                }
                            });
                }else {
                    processedAction();
                }

            }
        });
        return false;
    }

    /**
     * 悬浮窗权限
     * @return
     */
    private boolean processFloatPermission() {
        if (!FloatWindowManager.getInstance().checkPermission(this)){
            showAction(StringUtil.getString(R.string.float_permission), "我已授权", new Runnable() {
                @Override
                public void run() {
                    if (FloatWindowManager.getInstance().checkPermission(PermissionDialogActivity.this)) {
                        processedAction();
                    } else {
                        Toast.makeText(PermissionDialogActivity.this, "悬浮窗权限未获取", Toast.LENGTH_SHORT).show();
                    }
                }
            }, "确定", new Runnable() {
                @Override
                public void run() {
                    FloatWindowManager.getInstance().applyPermissionDirect(PermissionDialogActivity.this);
                }
            });
            return false;
        }
        return true;
    }

    private boolean processDynamicPermission(GroupPermission permission) {
        //动态申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            String[] requestPermissions = permission.permissions.toArray(new String[0]);
            //检查未被授权的权限
            final List<String> ungrantedPermissions = PermissionUtil.checkUngrantedPermission(this,requestPermissions);
            if(ungrantedPermissions != null && ungrantedPermissions.size() > 0){
                List<String> mappedName = new ArrayList<>();
                for (String dynPermission : ungrantedPermissions){
                    String mapName = PERMISSION_NAMES.get(dynPermission);
                    if (mapName != null) {
                        mappedName.add(mapName);
                    } else {
                        mappedName.add(dynPermission);
                    }
                }
                String permissionNames = StringUtil.join("、", mappedName);
                showAction(StringUtil.getString(R.string.request_dynamic_permission, permissionNames, ungrantedPermissions.size()),
                        "确定", new Runnable() {
                            @Override
                            public void run() {
                                ActivityCompat.requestPermissions(PermissionDialogActivity.this, ungrantedPermissions.toArray(new String[0]), M_PERMISSION_REQUEST);
                            }
                        }, "取消", new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(PermissionDialogActivity.this," 用户取消授权", Toast.LENGTH_SHORT).show();
                                PermissionUtil.onPermissionResult(currentPermissionIdx, false, "用户不进行授权");
                                finish();
                            }
                        });
                return false;
            }
        }
        return true;
    }

    private Runnable positiveAction;
    private Runnable negativeAction;

    @Override
    public void onClick(View v) {
        if (v == positiveButton) {
            if (positiveAction != null) {
                positiveAction.run();
            }
        } else if (v == negativeButton) {
            if (negativeAction != null) {
                negativeAction.run();
            }
        }
    }


    /**
     * 权限分组
     */
    private void groupPermissions() {
        List<String> permissions = getIntent().getStringArrayListExtra(PERMISSIONS_KEY);
        Map<Integer, GroupPermission> currentPermissions = new LinkedHashMap<>();

        // 按照分组过一遍
        for (String permission : permissions) {
            int group;
            switch (permission) {
                case "float":
                    group = PERMISSION_FLOAT;
                    break;
                case "root":
                    group = PERMISSION_ROOT;
                    break;
                case "adb":
                    group = PERMISSION_ADB;
                    break;
                case Settings.ACTION_USAGE_ACCESS_SETTINGS:
                    group = PERMISSION_USAGE;
                    break;
                case Settings.ACTION_ACCESSIBILITY_SETTINGS:
                    group = PERMISSION_ACCESSIBILITY;
                    break;
                case "screenRecord":
                    group = PERMISSION_RECORD;
                    break;
                default:
                    if (permission.startsWith("Android=")) {
                        group = PERMISSION_ANDROID;
                    } else if (permission.startsWith("toast:")) {
                        group = PERMISSION_TOAST;
                    } else {
                        group = PERMISSION_DYNAMIC;
                    }
                    break;
            }

            // 如果有同分组
            GroupPermission permissionG = currentPermissions.get(group);
            if (permissionG == null) {
                permissionG = new GroupPermission(group);
                currentPermissions.put(group, permissionG);
            }

            permissionG.addPermission(permission);
        }

        // 设置下实际需要的权限
        allPermissions = new ArrayList<>(currentPermissions.values());
    }

    /**
     * 权限分组
     */
    private static class GroupPermission {
        private int permissionType;
        private List<String> permissions;

        private GroupPermission(int permissionType) {
            this.permissionType = permissionType;
        }

        /**
         * 添加一条权限
         * @param permission
         */
        private void addPermission(String permission) {
            if (permissions == null) {
                permissions = new ArrayList<>();
            }

            if (!permissions.contains(permission)) {
                permissions.add(permission);
            } else {
                LogUtil.w(TAG, "Permission %s already added", permission);
            }
        }
    }
}
