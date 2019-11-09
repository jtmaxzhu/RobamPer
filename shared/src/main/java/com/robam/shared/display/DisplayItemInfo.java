package com.robam.shared.display;

import com.robam.shared.R;
import com.robam.shared.display.items.base.DisplayItem;
import com.robam.shared.display.items.base.Displayable;

import java.util.Arrays;
import java.util.List;

import static com.robam.shared.R.drawable.performance_icon;

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
