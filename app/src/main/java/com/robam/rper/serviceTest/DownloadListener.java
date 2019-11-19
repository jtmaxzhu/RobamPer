package com.robam.rper.serviceTest;

/**
 * author : liuxiaohu
 * date   : 2019/11/19 9:35
 * desc   :
 * version: 1.0
 */
public interface DownloadListener {
    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();
}
