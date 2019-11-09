package com.robam.rper.activity;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.robam.rper.R;
import com.robam.rper.ui.HeadControlPanel;

import java.io.IOException;

public class AdbSettingActivity extends AppCompatActivity {


    public static final String TAG = "AdbSettingActivity";

    private TextView mTips;

    private boolean mIsConnected = false;
    private HeadControlPanel mPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adbsetting);
        initViews();
        initHeadPanel();
        initTips(getWifiIpAdress());
    }


    private void initHeadPanel(){
        mPanel = (HeadControlPanel) findViewById(R.id.head_layout);
        mPanel.setMiddleTitle("ADB远程调试");
        mPanel.setBackIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    private void initViews() {
        mTips = (TextView) findViewById(R.id.tv_tips);
    }

    private void initTips(String wifiIpAddress) {
        if (!TextUtils.isEmpty(wifiIpAddress)) {
            mTips.setText("PC Exec: adb connect " + wifiIpAddress);
        } else {
            mTips.setText("Please check your connection!");
        }
    }

    private String getWifiIpAdress() {
        String wifiIpAddress = null;
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        if (wifiInfo != null) {
            int address = wifiInfo.getIpAddress();
            if (address != 0) {
                wifiIpAddress = Formatter.formatIpAddress(address);
            }
        }
        return wifiIpAddress;
    }

    public void start(View view) {
        if (setAdbTcpPort(5555)) {
            Toast.makeText(AdbSettingActivity.this, "开启成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(AdbSettingActivity.this, "开始失败", Toast.LENGTH_SHORT).show();
        }
    }

    public void stop(View view) {
        if (setAdbTcpPort(-1)) {
            Toast.makeText(AdbSettingActivity.this, "停止成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(AdbSettingActivity.this, "停止失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String wifiIpAddress = getWifiIpAdress();
        if (!TextUtils.isEmpty(wifiIpAddress)) {
            mIsConnected = true;
        } else {
            mIsConnected = false;
        }
    }

    private boolean setAdbTcpPort(int port) {
        if (!mIsConnected) {
            Toast.makeText(AdbSettingActivity.this, "Please check your connection!", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            Runtime.getRuntime().exec("setprop service.adb.tcp.port " + port);
            Runtime.getRuntime().exec("stop adbd");
            Runtime.getRuntime().exec("start adbd");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
