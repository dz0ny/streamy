package xyz.dz0ny.streamy.remote;

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

    static synchronized Retrofit getStrmClient() {
        if (strmClient == null) {
            strmClient = new Retrofit
                    .Builder()
                    .baseUrl("http://localhost:9092/")
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build();
        }

        return strmClient;
    }
}