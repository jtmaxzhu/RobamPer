package com.robam.rper.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.service.SPService;
import com.robam.rper.ui.ColorFilterRelativeLayout;
import com.robam.rper.ui.HeadControlPanel;
import com.robam.rper.util.ClassUtil;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.PermissionUtil;
import com.robam.rper.util.StringUtil;
import com.robam.rper.util.SystemUtil;

import org.greenrobot.greendao.annotation.Index;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexActivity extends BaseActivity {

    private static final String TAG = IndexActivity.class.getSimpleName();
    private static final String DISPLAY_ALERT_INFO = "displayAlertInfo";

    private HeadControlPanel mPanel;
    private GridView mGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        mPanel = (HeadControlPanel) findViewById(R.id.head_layout);
        mPanel.setMiddleTitle(getString(R.string.app_name));
        mPanel.setInfoIconClickListener(R.drawable.icon_config, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(IndexActivity.this, AdbSettingActivity.class));
            }
        });
        mPanel.setBackIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mGridView = (GridView) findViewById(R.id.tools_grid);
        initData();
    }

    private void  initData(){
        Map<String, Entry> entryList = new HashMap<>();
        List<Class<? extends Activity>> activities = ClassUtil.findSubClass(Activity.class, EntryActivity.class);
        // 配置唯一entry
        for (Class<? extends Activity> activityClass: activities) {
            // 配置
            Entry target = new Entry(activityClass.getAnnotation(EntryActivity.class), activityClass);
            if (entryList.containsKey(target.name)) {
                if (entryList.get(target.name).level < target.level) {
                    entryList.put(target.name, target);
                }
            } else {
                entryList.put(target.name, target);
            }
        }

        List<Entry> entries = new ArrayList<>(entryList.values());
        // 从大到小排
        Collections.sort(entries, new Comparator<Entry>() {
            @Override
            public int compare(Entry o1, Entry o2) {
                return o1.index - o2.index;
            }
        });

        CustomAdapter adapter = new CustomAdapter(this, entries);
        if (entries.size() <= 3) {
            mGridView.setNumColumns(1);
        } else {
            mGridView.setNumColumns(2);
        }
        mGridView.setAdapter(adapter);

        // 有写权限，申请下
        //PatchRequest.updatePatchList();
    }

    private String getWifiAddress() {
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiMgr != null;
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        if(wifiInfo != null) {
            int ip = wifiInfo.getIpAddress();
            return intToIp(ip);
        }else {
            return "";
        }
    }

    public static String intToIp(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

/*    private void setDebug(int port) {
        try {
            Runtime.getRuntime().exec("su");
            File file = new File("/data/");
            file.canWrite();
            Runtime.getRuntime().exec("setprop service.adb.tcp.port " + port);
            Runtime.getRuntime().exec("stop adbd");
            Runtime.getRuntime().exec("start adbd");
            MyApplication.getInstance().showToast("adb connect \"" + getWifiAddress() + ":5555");
        } catch (Exception e) {
            MyApplication.getInstance().showToast("Can not get root permission");
        }
    }*/



    public static class Entry{
        private int iconId;
        private String name;
        private String[] permissions;
        private int level;
        private int index;
        private int cornerColor;
        private String cornerText;
        private float saturation;
        private int cornerPersist;
        private Class<? extends Activity> targetActivity;

        public Entry(EntryActivity activity, Class<? extends Activity> target) {
            this.iconId = activity.icon();
            this.name = activity.name();
            permissions = activity.permissions();
            level = activity.level();
            targetActivity = target;
            index = activity.index();
            cornerText = activity.cornerText();
            cornerColor = activity.cornerBg();
            cornerPersist = activity.cornerPersist();
            saturation = activity.saturation();
        }

    }

    public class CustomAdapter extends BaseAdapter{

        private final Context context;
        private final List<Entry> data;
        private LayoutInflater mInflater;

        JSONObject entryCount;
        JSONObject versionsCount;
        int currentVersionCode;

        public CustomAdapter(Context context, List<Entry> data) {
            this.context = context;
            this.data = data;
            mInflater = LayoutInflater.from(context);

            String appInfo = SPService.getString(SPService.KEY_INDEX_RECORD, null);
            currentVersionCode = SystemUtil.getAppVersionCode();
            if (appInfo == null){
                versionsCount = new JSONObject();
                entryCount = new JSONObject();
            }else {
                versionsCount = JSON.parseObject(appInfo);
                entryCount = versionsCount.getJSONObject(Integer.toString(currentVersionCode));
                if (entryCount == null) {
                    entryCount = new JSONObject();
                }
            }
        }


        @Override
        public int getCount() {
            if (data != null) {
                return data.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            if (data != null) {
                return data.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if (convertView == null){
                //将XML转化为View
                convertView = mInflater.inflate(R.layout.item_tools_grid, parent, false);
                viewHolder = new ViewHolder();
                //通过setTag将convertView与viewHolder关联
                convertView.setTag(viewHolder);
                //对viewHolder的属性进行赋值
                viewHolder.icon = (ImageView) convertView.findViewById(R.id.img);
                viewHolder.name = (TextView) convertView.findViewById(R.id.tv);
                viewHolder.corner = (TextView) convertView.findViewById(R.id.index_corner);
                viewHolder.background = (ColorFilterRelativeLayout) convertView;
            }else {
                //如果缓存池中有对应的view缓存，则直接通过getTag取出viewHolder
                viewHolder = (ViewHolder) convertView.getTag();
            }
            // 取出Entry对象
            final Entry entry = data.get(position);
            viewHolder.icon.setImageResource(entry.iconId);
            viewHolder.name.setText(entry.name);

            Integer itemCount = entryCount.getInteger(entry.name);
            if (itemCount == null) {
                itemCount = 0;
            }
            // 持续显示或者，有进入次数计数
            if (entry.cornerPersist == 0 ||
                    (entry.cornerPersist > 0 && itemCount < entry.cornerPersist)) {
                // 如果有角标配置，设置角标
                if (!StringUtil.isEmpty(entry.cornerText)) {
                    viewHolder.corner.setText(entry.cornerText);
                    viewHolder.corner.setBackgroundColor(entry.cornerColor);
                    viewHolder.corner.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.corner.setVisibility(View.GONE);
                }
            } else {
                viewHolder.corner.setVisibility(View.GONE);
            }
            if (entry.saturation != 1F) {
                viewHolder.background.setSaturation(entry.saturation);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PermissionUtil.requestPermissions(Arrays.asList(entry.permissions), IndexActivity.this, new PermissionUtil.OnPermissionCallback() {
                        @Override
                        public void onPermissionResult(boolean result, String reason) {
                            if (result){
  /*                               if (mPanel != null){
                                   Integer count = entryCount.getInteger(entry.name);
                                    if (count == null) {
                                        count = 1;
                                    } else {
                                        count ++;
                                    }
                                    entryCount.put(entry.name, count);
                                    versionsCount.put(Integer.toString(currentVersionCode), entryCount);
                                    SPService.putString(SPService.KEY_INDEX_RECORD, JSON.toJSONString(versionsCount));

                                    mPanel.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            LogUtil.d(TAG, entry.targetActivity.getName());
                                            Intent intent = new Intent(IndexActivity.this, entry.targetActivity);
                                            startActivity(intent);
                                        }
                                    });
                                }*/
                                LogUtil.d(TAG, entry.targetActivity.getName());
                                Intent intent = new Intent(IndexActivity.this, entry.targetActivity);
                                startActivity(intent);
                            }
                        }
                    });
                }
            });
            return convertView;
        }


        public List<Entry> getData() {
            return data;
        }


        // ViewHolder用于缓存控件，四个属性分别对应item布局文件的四个控件
        public class ViewHolder {
            ColorFilterRelativeLayout background;
            ImageView icon;
            TextView name;
            TextView corner;
        }
    }


}
