package com.wcedla.downloaddemo;

public interface downloadListener {

    String getFileName();

    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();
}
