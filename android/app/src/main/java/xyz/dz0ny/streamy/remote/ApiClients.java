package xyz.dz0ny.streamy.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class ApiClients {

    private static Retrofit popClient;
    private static Retrofit strmClient;

    public static synchronized Retrofit getPopApiClient() {
        if (popClient == null) {
            popClient = new Retrofit
                    .Builder()
                    .baseUrl("https://movies-v2.api-fetch.website/")
                    .addConverterFactory(MoshiConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
        }

        return popClient;
    }

    public static synchronized Retrofit getStrmClient() {
        if (strmClient == null) {
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(1, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();
            strmClient = new Retrofit
                    .Builder()
                    .client(okHttpClient)
                    .baseUrl("http://localhost:9092/")
                    .addConverterFactory(MoshiConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
        }

        return strmClient;
    }
}