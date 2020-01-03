package com.robam.rper.activity;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.robam.rper.R;
import com.robam.rper.adapter.PerformFloatAdapter;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.annotation.Param;
import com.robam.rper.injector.InjectorService;
import com.robam.rper.injector.param.SubscribeParamEnum;
import com.robam.rper.injector.param.Subscriber;
import com.robam.rper.tools.BackgroundExecutor;
import com.robam.rper.tools.CmdTools;
import com.robam.rper.ui.HeadControlPanel;
import com.robam.rper.util.ClassUtil;
import com.robam.rper.util.GlideUtil;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.PermissionUtil;
import com.robam.rper.util.StringUtil;

import java.util.List;


@EntryActivity(icon = R.drawable.xn, name = "性能工具", permissions = {"adb", "float"}, index = 1)
//@EntryActivity(icon = R.drawable.xn, name = "性能工具", index = 1)
public class PerformanceActivity extends BaseActivity {
    private HeadControlPanel mPanel;
    private String TAG = "PerformanceFragment";

    private ListView mFloatListView;
    private ListView mStressListView;
    private PerformFloatAdapter mPerfFloatAdapter;

    /**
     * 目标应用包名
     */
    private String app;

    @Subscriber(@Param(SubscribeParamEnum.APP))
    public void setApp(String app) {
        this.app = app;
    }

/*    @Subscriber(@Param(SubscribeParamEnum.APP_NAME))
    public void testdemoProprivate(final String appName){
        LogUtil.d("PerFloatService","appName"+appName);
    }*/


    @Override
    protected void onDestroy() {
        InjectorService injectorService = MyApplication.getInstance().findServiceByName(InjectorService.class.getName());
        injectorService.unregister(this);
        mPerfFloatAdapter.unRegister();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);

        InjectorService injectorService = MyApplication.getInstance().findServiceByName(InjectorService.class.getName());
        injectorService.register(this);

        mPerfFloatAdapter = new PerformFloatAdapter(this);

        mPanel = (HeadControlPanel) findViewById(R.id.head_layout);
        mPanel.setMiddleTitle("性能测试");
        mPanel.setBackIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //性能监听列表
        mFloatListView = (ListView) findViewById(R.id.perform_float_list);
        mFloatListView.setAdapter(mPerfFloatAdapter);
        mFloatListView.setDivider(new ColorDrawable(getResources().getColor(R.color.divider_color)));
        mFloatListView.setDividerHeight(1);
        mFloatListView.setFooterDividersEnabled(false);
        mFloatListView.setHeaderDividersEnabled(false);

        final List<ApplicationInfo> listPack = MyApplication.getInstance().loadAppList();

        AppCompatSpinner spinner = (AppCompatSpinner) findViewById(R.id.perform_param_spinner);

        spinner.setAdapter(new SpinnerAdapter() {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    v = LayoutInflater.from(PerformanceActivity.this).inflate(R.layout.activity_choose_layout, null);
                }
                if (position == 0) {
                    ImageView img = (ImageView) v.findViewById(R.id.choose_icon);
                    img.setImageResource(R.drawable.icon_global);
                    TextView title = (TextView) v.findViewById(R.id.choose_title);
                    title.setText(R.string.global);
                    TextView activity = (TextView) v.findViewById(R.id.choose_activity);
                    activity.setText("");
                } else {
                    ApplicationInfo info = listPack.get(position - 1);
                    ImageView img = (ImageView) v.findViewById(R.id.choose_icon);
                    GlideUtil.loadIcon(PerformanceActivity.this, info.packageName, img);
                    TextView title = (TextView) v.findViewById(R.id.choose_title);
                    title.setText(info.loadLabel(getPackageManager()).toString());
                    TextView activity = (TextView) v.findViewById(R.id.choose_activity);
                    activity.setText(info.packageName);
                }
                return v;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public int getCount() {
                return listPack.size() + 1;
            }

            @Override
            public Object getItem(int position) {
                if (position == 0) {
                    return 0;
                }
                return listPack.get(position - 1);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return getDropDownView(position, convertView, parent);
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });

        int position = -1;

        if (!StringUtil.isEmpty(app)) {
            for (int i = 0; i < listPack.size(); i++) {
                if (StringUtil.equals(app, listPack.get(i).packageName)) {
                    position = i;
                    break;
                }
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 全局特殊处理
                if (position == 0) {
                    ((MyApplication)getApplication()).updateAppAndName("-", "全局");
                } else {
                    ApplicationInfo info = listPack.get(position - 1);
                    LogUtil.i(TAG, "Select info: " + StringUtil.hide(info.packageName));

                    ((MyApplication)getApplication()).updateAppAndName(info.packageName, info.loadLabel(getPackageManager()).toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner.setSelection(position + 1);

       // final View screenRecordBtn = findViewById(R.id.screen_record_btn);

/*        screenRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    toastShort("此功能不支持Android5.0以下设备");
                    return;
                }

                if (ClassUtil.getPatchInfo(VideoAnalyzer.SCREEN_RECORD_PATCH) == null) {
                    MyApplication.getInstance().showDialog(PerformanceActivity.this, "是否加载录屏耗时计算插件?", "是", new Runnable() {
                        @Override
                        public void run() {
                            showProgressDialog("插件下载中");
                            BackgroundExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    PatchLoadResult rs = AssetsManager.loadPatchFromServer(VideoAnalyzer.SCREEN_RECORD_PATCH, new PrepareUtil.PrepareStatus() {
                                        @Override
                                        public void currentStatus(int progress, int total, String message, boolean status) {
                                            updateProgressDialog(progress, total, message);
                                        }
                                    });
                                    if (rs == null) {
                                        // 降级到网络模式
                                        dismissProgressDialog();
                                        toastLong("无法加载计算插件");
                                        return;
                                    }

                                    dismissProgressDialog();
                                    screenRecordBtn.callOnClick();
                                }
                            });

                        }
                    }, "否", null);
                    return;
                }

                if (!PermissionUtil.isFloatWindowPermissionOn(PerformanceActivity.this)) {
                    return;
                }

                PermissionUtil.grantHighPrivilegePermissionAsync(new CmdTools.GrantHighPrivPermissionCallback() {
                    @Override
                    public void onGrantSuccess() {
                        startActivity(new Intent(PerformanceActivity.this, RecorderConfigActivity.class));
                    }

                    @Override
                    public void onGrantFail(String msg) {
                        toastLong("设备需要开启ADB 5555端口并授权调试才可使用" +
                                "\n请在命令行执行 adb tcpip 5555");
                    }
                });
            }
        });*/

      /*  LinearLayout button = (LinearLayout) findViewById(R.id.chart_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PerformanceActivity.this, PerformanceChartActivity.class);
                startActivity(intent);
            }
        });*/

    }
}
