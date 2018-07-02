package xyz.dz0ny.streamy.remote;

import java.util.List;

import io.reactivex.Observable;
import xyz.dz0ny.streamy.remote.popcorn.PopcornAPI;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornMovie;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornShow;
import xyz.dz0ny.streamy.remote.popcorn.models.ShowDetails;
import xyz.dz0ny.streamy.remote.streamy.StreamyAPI;
import xyz.dz0ny.streamy.remote.streamy.models.StreamyTorrent;

public class ApiCalls {

    public static Observable<List<PopcornMovie>> getMovies(String nextPage, String sort) {
        return PopcornAPI.client().movies(nextPage, sort, null);
    }

    public static Observable<List<PopcornShow>> getShows(String nextPage, String sort) {
        return PopcornAPI.client().shows(nextPage, sort, null);
    }

    public static Observable<List<PopcornMovie>> getMoviesSearch(String nextPage, String sort, String q) {
        return PopcornAPI.client().movies(nextPage, sort, q);
    }

    public static Observable<List<PopcornShow>> getShowsSearch(String nextPage, String sort, String q) {
        return PopcornAPI.client().shows(nextPage, sort, q);
    }

    public static Observable<ShowDetails> getShow(String id) {
        return PopcornAPI.client().show(id);
    }

    public static Observable<StreamyTorrent> addTorrent(String magnet) {
        return StreamyAPI.client().addMagnet(magnet);
    }

    public static Observable<List<StreamyTorrent>> getTorrents() {
        return StreamyAPI.client().getTorrents();
    }
}