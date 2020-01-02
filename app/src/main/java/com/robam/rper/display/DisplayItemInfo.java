package com.robam.rper.display;

import com.robam.rper.R;
import com.robam.rper.display.items.base.DisplayItem;
import com.robam.rper.display.items.base.Displayable;

import java.util.Arrays;
import java.util.List;

/**
 * author : liuxiaohu
 * date   : 2019/11/8 14:17
 * desc   :
 * version: 1.0
 */
public class DisplayItemInfo {
    /**
     * 名称
     */
    private final String name;

    /**
     * 依赖权限
     */
    private final List<String> permissions;

    /**
     * 提示文案
     */
    private final String tip;

    /**
     * 图标
     */
    private final int icon;

    /**
     * level信息
     */
    protected final int level;

    /**
     * 触发文案
     */
    private final String trigger;

    /**
     * 目标类
     */
    private final Class<? extends Displayable> targetClass;


    public DisplayItemInfo(DisplayItem displayItem, Class<? extends Displayable> targetClass) {
        /**DEBUG内存数据案例
         * this.targetClass = class com.robam.rper.display.items.CPUTools;
         * this.name = "CPU";
         * this.permissions = {"adb", "float"};
         * this.tip = "";
         * this.icon = "2131165289";
         *this.level = 0;
         *this.trigger = "";
         */
        this.targetClass = targetClass;
        this.name = displayItem.name();
        this.permissions = Arrays.asList(displayItem.permissions());
        this.tip = displayItem.tip();
        if (displayItem.icon() != 0) {
            this.icon = displayItem.icon();
        } else {
            this.icon = R.drawable.performance_icon;
        }
        this.level = displayItem.level();
        this.trigger = displayItem.trigger();
    }

    public String getName() {
        return name;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public String getTip() {
        return tip;
    }

    public int getIcon() {
        return icon;
    }

    public String getTrigger() {
        return trigger;
    }

    public Class<? extends Displayable> getTargetClass() {
        return targetClass;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DisplayItemInfo info = (DisplayItemInfo) obj;
        return targetClass.equals(info.targetClass);
    }
}
