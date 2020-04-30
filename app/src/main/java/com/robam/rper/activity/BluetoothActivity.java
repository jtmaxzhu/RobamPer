package com.robam.rper.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.util.LogUtil;

@EntryActivity(icon = R.drawable.xn, name = "蓝牙检测", index = 5)
public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener {
    BluetoothAdapter bAdapter;
    EditText nameView;//声明edittext
    String blueName;//声明用户输入的蓝牙设备名称变量
    TextView showRssi;//声明textview用于显示信号强度信息
    public static FoundReceiver fr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        bAdapter = BluetoothAdapter.getDefaultAdapter();//获取蓝牙适配器
        //设置过滤器，过滤因远程蓝牙设备被找到而发送的广播 BluetoothDevice.ACTION_FOUND
        IntentFilter iFilter=new IntentFilter();
        iFilter.addAction(BluetoothDevice.ACTION_FOUND);
        //设置广播接收器和安装过滤器
        fr = new FoundReceiver();
        registerReceiver(fr, iFilter);
        //获取控件对象
        nameView = (EditText) findViewById(R.id.bluetoothName);
        showRssi = (TextView) findViewById(R.id.showRssi);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(fr);
    }

    /**
     * 打开蓝牙
     * @param v
     */
    @SuppressLint("WrongConstant")
    public void open(View v) {
        if(!bAdapter.isEnabled()){
            bAdapter.enable();
            Toast.makeText(getApplicationContext(), "蓝牙打开成功", 0).show();
        }else{
            Toast.makeText(getApplicationContext(), "蓝牙已经打开", 0).show();
        }
    }

    /**
     * 关闭蓝牙
     * @param v
     */
    @SuppressLint("WrongConstant")
    public void close(View v) {
        if(bAdapter.isEnabled()){
            bAdapter.disable();
            Toast.makeText(getApplicationContext(), "蓝牙关闭成功", 0).show();
        }else{
            Toast.makeText(getApplicationContext(), "蓝牙已经关闭", 0).show();
        }
    }

    /**
     * 搜索远程蓝牙设备，获取editview的值
     * @param v
     */
    @SuppressLint("WrongConstant")
    public void show(View v) {
        if(bAdapter.isEnabled()){
            blueName = nameView.getText().toString().trim();
            bAdapter.startDiscovery();
        }else{
            Toast.makeText(getApplicationContext(), "蓝牙未打开", 0).show();;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.open:
                open(v);
            case R.id.close:
                close(v);
            case R.id.show:
                show(v);
        }
    }


    /**
     * 内部类：当找到一个远程蓝牙设备时执行的广播接收者
     * @author Administrator
     *
     */
    class FoundReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);//获取此时找到的远程设备对象
            LogUtil.d("Blue","蓝牙名称："+device.getName());
            short rssi=intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
            showRssi.setText(device.getName()+":"+rssi);
//            if(blueName.equals(device.getName())){//判断远程设备是否与用户目标设备相同
//                //short rssi=intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);//获取额外rssi值
//                //showRssi.setText(device.getName()+":"+rssi);//显示rssi到控件上
//                //bAdapter.cancelDiscovery();//关闭搜索
//            }else{
//                showRssi.setText("未发现设备“"+blueName+"”");
//            }
            LogUtil.d("Blue","退出");
        }

    }

}
