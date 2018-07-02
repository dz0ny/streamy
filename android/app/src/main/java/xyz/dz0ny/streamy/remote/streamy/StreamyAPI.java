package xyz.dz0ny.streamy.remote.streamy;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import xyz.dz0ny.streamy.remote.ApiClients;
import xyz.dz0ny.streamy.remote.streamy.models.StreamyTorrent;

public class StreamyAPI {

    private static StreamyService mDummyService;

    public static synchronized StreamyService client() {
        if (mDummyService == null) {
            mDummyService = ApiClients
                    .getStrmClient()
                    .create(StreamyService.class);
        }

        return mDummyService;
    }

    public interface StreamyService {

        @GET("/torrents/add")
        Observable<StreamyTorrent> addMagnet(@Query("magnet") String magnet);

        @GET("/torrents/{hash}")
        Observable<StreamyTorrent> getTorrent(@Path("hash") String torrent);

        @GET("/torrents/{hash}/start")
        Observable<StreamyTorrent> startTorrent(@Path("hash") String torrent);

        @GET("/torrents/{hash}/stop")
        Observable<StreamyTorrent> stopTorrent(@Path("hash") String torrent);

        @DELETE("/torrents/{hash}")
        Observable<StreamyTorrent> deleteTorrent(@Path("hash") String torrent);

        @GET("/torrents/")
        Observable<List<StreamyTorrent>> getTorrents();
    }
}
