package com.robam.rper.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.ui.SlideDeleteCancelListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//@EntryActivity(icon = R.drawable.xn, name = "列表演示2", index = 3)
public class ListMyActivity extends AppCompatActivity {
    private SlideDeleteCancelListView mListView = null;
    private List<String> mDatas = null;
    private ArrayAdapter mAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sidle_list);
        mListView = (SlideDeleteCancelListView) findViewById(R.id.list_myView);
        mDatas = new ArrayList<>(Arrays.asList("北京市", "天津市", "上海市", "重庆市", "安徽省", "福建省",
                "甘肃省", "广东省", "贵州省", "海南省", "河北省","河南省","黑龙江省","湖北省","湖南省","吉林省"));
        mAdapter = new ArrayAdapter(ListMyActivity.this,android.R.layout.simple_list_item_1, mDatas);
        mListView.setAdapter(mAdapter);

        //设置列表项Item删除按钮的点击监听事件
        mListView.setDelButtonClickListener(new SlideDeleteCancelListView.DelButtonClickListener() {
            @Override
            public void onDelClick(int position) {
                Toast.makeText(ListMyActivity.this, "删除：" + position + " : " + mAdapter.getItem(position), Toast.LENGTH_SHORT).show();
                //从列表中移除当前项
                mAdapter.remove(mAdapter.getItem(position));
            }
        });

        //设置列表项Item的取消按钮点击监听事件
        mListView.setCancelButtonClickListener(new SlideDeleteCancelListView.CancelButtonClickListener() {
            @Override
            public void onCancelClick(int position) {
                Toast.makeText(ListMyActivity.this, "取消关注：" + position + " : " + mAdapter.getItem(position), Toast.LENGTH_SHORT).show();
            }
        });

        //设置列表项Item点击监听事件
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Toast.makeText(ListMyActivity.this, "您点击的是第 " + position + " 项: " + mAdapter.getItem(position), Toast.LENGTH_SHORT).show();
            }
        });
    }


}
