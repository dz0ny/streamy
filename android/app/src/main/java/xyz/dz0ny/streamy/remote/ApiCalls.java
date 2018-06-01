package xyz.dz0ny.streamy.remote;

import java.util.List;

import io.reactivex.Observable;
import xyz.dz0ny.streamy.remote.popcorn.PopcornAPI;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornMovie;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornShow;
import xyz.dz0ny.streamy.remote.popcorn.models.ShowDetails;

public class ApiCalls {

    public static Observable<List<PopcornMovie>> getMovies(String nextPage, String sort) {
        return PopcornAPI.getPopcornService().movies(nextPage, sort);
    }

    public static Observable<List<PopcornShow>> getShows(String nextPage, String sort) {
        return PopcornAPI.getPopcornService().shows(nextPage, sort);
    }

    public static Observable<ShowDetails> getShow(String id) {
        return PopcornAPI.getPopcornService().show(id);
    }
}