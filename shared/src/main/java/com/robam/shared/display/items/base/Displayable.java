package com.robam.shared.display.items.base;

import java.util.List;
import java.util.Map;

/**
 * author : liuxiaohu
 * date   : 2019/11/8 14:18
 * desc   :
 * version: 1.0
 */
public interface Displayable {
    void start();

    void stop();

    /**
     * 实时数据获取
     *
     * @return 实时数据
     * @throws Exception
     */
    String getCurrentInfo() throws Exception;

    /**
     * 获取最小刷新间隔
     *
     * @return
     */
    long getRefreshFrequency();

    /**
     * 清理方法
     */
    void clear();

    /**
     * 开始录制
     */
    void startRecord();

    /**
     * 调用录制
     */
    void record();

    /**
     * 触发特定事件
     */
    void trigger();

    /**
     * 停止录制并返回录制数据
     * @return
     */
    Map<RecordPattern, List<RecordPattern.RecordItem>> stopRecord();
}
