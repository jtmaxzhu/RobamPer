package com.robam.rper.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.LayoutInflaterCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.robam.rper.R;
import com.robam.rper.activity.MyApplication;
import com.robam.rper.annotation.Param;
import com.robam.rper.annotation.Provider;
import com.robam.rper.display.DisplayItemInfo;
import com.robam.rper.display.DisplayProvider;
import com.robam.rper.injector.InjectorService;
import com.robam.rper.injector.param.RunningThread;
import com.robam.rper.injector.param.SubscribeParamEnum;
import com.robam.rper.injector.param.Subscriber;
import com.robam.rper.service.DisplayManager;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.PermissionUtil;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * author : liuxiaohu
 * date   : 2019/12/9 11:15
 * desc   :
 * version: 1.0
 */
public class PerformFloatAdapter extends BaseAdapter {
    private String TAG = "PerformFloatAdapter";
    private Activity context;
    private LayoutInflater layoutInflater;
    private List<DisplayItemInfo> mData;

    private DisplayProvider provider;
    private DisplayManager displayManager;

    private Map<Integer, Boolean> isSelected;

   /* private  static PerformFloatAdapter mInstance;

    public static PerformFloatAdapter getInstance(Activity context){
        if (mInstance == null){
            mInstance = new PerformFloatAdapter(context);
        }
        return mInstance;
    }*/

   public void unRegister(){
       InjectorService injectorService = MyApplication.getInstance().findServiceByName(InjectorService.class.getName());
       injectorService.unregister(this);
   }



    public PerformFloatAdapter(Activity context) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        init();
    }

    private void init() {
        mData = new ArrayList<>();
        displayManager = DisplayManager.getInstance();
        provider = MyApplication.getInstance().findServiceByName(DisplayProvider.class.getName());
        mData = provider.getAllDisplayItems();

        Set<String> runningItems = provider.getRunningDisplayItems();

        isSelected = new HashMap<>();//设置listitem勾选状态，初始全为false
        for (int i = 0; i < mData.size() ; i++) {
            if (runningItems.contains(mData.get(i).getName())){
                isSelected.put(i,true);
            }else {
                isSelected.put(i, false);
            }
        }
        InjectorService injectorService = MyApplication.getInstance().findServiceByName(InjectorService.class.getName());
        injectorService.register(this);
        /**内存数据
         * mData = {DisplayItemInfo}
         *       ->->name="CPU"
         *          *      ->level=1
         *          *      ->targetClass=class com.robam.rper.display.items.CPUTools
         *          *      ->...
         */
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null){
            viewHolder = new ViewHolder();
            convertView = layoutInflater.inflate(R.layout.perform_float_list, null);
            viewHolder.img = convertView.findViewById(R.id.img);
            viewHolder.title = convertView.findViewById(R.id.title);
            viewHolder.cBox = convertView.findViewById(R.id.cb);
            viewHolder.cBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final int position = (int)v.getTag();
                    if (!isSelected.get(position)){
                        //权限检查
                        Set<String> permissions = new HashSet<>();
                        permissions.addAll(mData.get(position).getPermissions());
                        PermissionUtil.requestPermissions(new ArrayList<>(permissions), context, new PermissionUtil.OnPermissionCallback() {
                            @Override
                            public void onPermissionResult(boolean result, String reason) {
                                if (result){
                                    List<DisplayItemInfo> addItem = new ArrayList<>();
                                    addItem.add(mData.get(position));
                                    isSelected.put(position, true);
                                    List<DisplayItemInfo> fail = displayManager.updateRecordingItems(addItem, null);
                                    if (fail != null && fail.size() > 0){
                                        for (DisplayItemInfo failed: fail) {
                                            LogUtil.d(TAG, "Open item %s failed", failed.getName());
                                            int idx = mData.indexOf(failed);
                                            isSelected.put(idx, false);
                                        }
                                    }
                                }
                                ((CheckBox) v).setChecked(isSelected.get(position));
                            }
                        });
                    }else{
                        List<DisplayItemInfo> removeItem = new ArrayList<>(2);
                        removeItem.add(mData.get(position));
                        isSelected.put(position, false);
                        displayManager.updateRecordingItems(null, removeItem);
                        ((CheckBox) v).setChecked(isSelected.get(position));
                    }
                }
            });
            viewHolder.tip = convertView.findViewById(R.id.tip);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.img.setImageResource(mData.get(position).getIcon());
        viewHolder.title.setText(mData.get(position).getName());
        viewHolder.tip.setText(mData.get(position).getTip());
        viewHolder.cBox.setChecked(isSelected.get(position));
        viewHolder.cBox.setTag(position);
        return convertView;
    }


    public static final class ViewHolder {
        public ImageView img;
        public TextView title;
        public TextView tip;
        public CheckBox cBox;
    }
    @Subscriber(value = @Param(DisplayManager.STOP_DISPLAY), thread = RunningThread.MAIN_THREAD)
    public void onDisplayStop() {
        LogUtil.d("testdemo", "进入onDisplayStop");
        for (int i = 0; i < mData.size(); i++) {
            isSelected.put(i, false);
        }
        notifyDataSetChanged();
    }



}
