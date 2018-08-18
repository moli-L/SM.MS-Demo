package com.example.smmsdemo.service;

import com.example.smmsdemo.JsonBean;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface Api {
    @Headers("user-agent: SMMSDemo/1.0")
    @Multipart
    @POST("upload")
    Observable<JsonBean> upload(@Part MultipartBody.Part part);

    @Headers("user-agent: SMMSDemo/1.0")
    @GET("clear")
    Observable<JsonBean> clear();
}
