package com.robam.rper.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * author : liuxiaohu
 * date   : 2019/11/5 10:18
 * desc   : 重写listView这样能够有滑动效果，使用默认listview只能显示一行
 * version: 1.0
 */
public class TotalMeasureListView extends ListView {
    public TotalMeasureListView(Context context) {
        super(context);
    }

    public TotalMeasureListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TotalMeasureListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 1,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
