package xyz.dz0ny.streamy.remote.popcorn;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import xyz.dz0ny.streamy.remote.ApiClients;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornMovie;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornShow;
import xyz.dz0ny.streamy.remote.popcorn.models.ShowDetails;

public class PopcornAPI {

    private static PopcornService mDummyService;

    public static synchronized PopcornService getPopcornService() {
        if (mDummyService == null) {
            mDummyService = ApiClients
                    .getPopApiClient()
                    .create(PopcornService.class);
        }

        return mDummyService;
    }

    public interface PopcornService {

        @GET("movies/{page}")
        Observable<List<PopcornMovie>> movies(@Path("page") String nextPage, @Query("sort") String sort);

        @GET("shows/{page}")
        Observable<List<PopcornShow>> shows(@Path("page") String nextPage, @Query("sort") String sort);

        @GET("show/{page}")
        Observable<ShowDetails> show(@Path("page") String id);

    }
}