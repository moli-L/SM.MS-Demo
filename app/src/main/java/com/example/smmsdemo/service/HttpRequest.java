package com.example.smmsdemo.service;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpRequest {
    private static OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS);

    private static Api api;

    public static Api getApi(){
        if(api == null){
            api = buildRetrofit().create(Api.class);
        }
        return api;
    }

    private static Retrofit buildRetrofit(){
        return new Retrofit.Builder()
                .client(clientBuilder.build())
                .baseUrl("https://sm.ms/api/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
