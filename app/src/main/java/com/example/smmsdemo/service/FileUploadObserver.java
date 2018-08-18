package com.example.smmsdemo.service;

import io.reactivex.Observer;

/**
 * Created by 黎隆海 on 2018/7/22.
 */

public abstract class FileUploadObserver<T> implements Observer<T> {
    @Override
    public void onNext(T t) {
        onSuccess(t);
    }

    @Override
    public void onError(Throwable e) {
        onFail(e);
    }

    @Override
    public void onComplete() {
    }

    //监听进度的改变
    public void onProgressChange(long bytesWritten, long contentLength) {
        onProgress((int) (bytesWritten*100 / contentLength));
    }

    //上传成功的回调
    public abstract void onSuccess(T t);

    //上传失败回调
    public abstract void onFail(Throwable e);

    //上传进度回调
    public abstract void onProgress(int progress);
}
