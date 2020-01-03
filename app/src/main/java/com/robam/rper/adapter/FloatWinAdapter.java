package com.robam.rper.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.robam.rper.R;
import com.robam.rper.display.DisplayItemInfo;
import com.robam.rper.service.DisplayManager;
import com.robam.rper.util.StringUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * author : liuxiaohu
 * date   : 2019/11/27 13:54
 * desc   :
 * version: 1.0
 */
public class FloatWinAdapter extends RecyclerView.Adapter<FloatWinAdapter.InformationViewHolder> {
    private LayoutInflater mInflater;
    private String TAG = "FloatWinAdapter";
    private List<DisplayItemInfo> listViewData;
    private List<String> contents;
    private WeakReference<DisplayManager> manRef;
    Context context;



    public FloatWinAdapter(Context context, DisplayManager manager, List<DisplayItemInfo> listViewData) {
        mInflater = LayoutInflater.from(context);
        this.context = context;
        this.manRef = new WeakReference<>(manager);
        this.listViewData = listViewData;
        this.contents = new ArrayList<>();
    }

    /**
     * 创建ViewHolder实例，将float_win_list布局传入,创建ViewHolder实例
     * @param viewGroup
     * @param i
     * @return
     */
    @Override
    public InformationViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.float_win_list, null);
        InformationViewHolder viewHolder = new InformationViewHolder(view, manRef.get());
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        return viewHolder;
    }

    /**
     * 对RecyclerView子项的数据进行赋值，会在每个子项被滚动到屏幕内执行
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(InformationViewHolder holder, int position) {
        DisplayItemInfo info = listViewData.get(position);
        if (contents == null || contents.size() <= position){
            holder.updateViewContent(info,null);
        }else {
            holder.updateViewContent(info, contents.get(position));
        }

    }

    public void updateListViewSource(List<DisplayItemInfo> infoList, List<String> messages){
        this.listViewData = infoList;
        this.contents = messages;
        //强制刷新RecyClerView列表
        notifyDataSetChanged();
        /**
         *  this.listViewData{ArrayList@xxxx} = (0=displayItemInfo = (name="CPU"，targetClass=class com.robam.rper.display.items.CPUTools))
         *  this.contents{ArrayList@xxxx} = 全局:8.27%
         */
    }


    /**
     * 返回子项的数量
     * @return
     */
    @Override
    public int getItemCount() {
        return listViewData.size();
    }




     static final class InformationViewHolder extends RecyclerView.ViewHolder{
        private TextView content;
        private TextView appTitle;
        private TextView trigger;
        private DisplayItemInfo currentInfo;

        private InformationViewHolder(View itemView, final DisplayManager manager) {
            super(itemView);
            content = itemView.findViewById(R.id.display_content);
            appTitle = itemView.findViewById(R.id.display_title);
            trigger = itemView.findViewById(R.id.display_trigger);
            trigger.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    manager.triggerInfo(currentInfo);
                }
            });
        }

        private void updateViewContent(DisplayItemInfo info, final String content){
            this.appTitle.setText(info.getName());
            this.content.setText(content);
            if (!StringUtil.isEmpty(info.getTrigger())){
                this.trigger.setText(info.getTrigger());
                this.trigger.setVisibility(View.VISIBLE);
                this.currentInfo = info;
            }else {
                this.trigger.setVisibility(View.GONE);
            }
        }
    }
}
