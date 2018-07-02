package xyz.dz0ny.streamy.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;

import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import xyz.dz0ny.streamy.activity.DetailsActivity;
import xyz.dz0ny.streamy.activity.ShowActivity;
import xyz.dz0ny.streamy.adapter.MoviesAdapterPopcorn;
import xyz.dz0ny.streamy.adapter.PopcornPaginationAdapter;
import xyz.dz0ny.streamy.adapter.ShowsAdapterPopcorn;
import xyz.dz0ny.streamy.remote.ApiCalls;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornMovie;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornShow;

public class PopcornSearchFragment extends android.support.v17.leanback.app.SearchFragment
        implements android.support.v17.leanback.app.SearchFragment.SearchResultProvider {

    private ArrayObjectAdapter mRowsAdapter;
    private MoviesAdapterPopcorn movies;
    private ShowsAdapterPopcorn shows;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        setSearchResultProvider(this);

        setOnItemViewClickedListener(this::getOnItemViewClickedListener);
    }

    private void getOnItemViewClickedListener(
            Presenter.ViewHolder itemViewHolder,
            Object item,
            RowPresenter.ViewHolder rowViewHolder,
            Row row
    ) {

        if (item instanceof PopcornMovie) {
            PopcornMovie movie = (PopcornMovie) item;
            Timber.d("Item: %s", item.toString());
            Intent intent = new Intent(getActivity(), DetailsActivity.class);
            intent.putExtra(DetailsActivity.MOVIE, movie);

            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),
                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                    DetailsActivity.SHARED_ELEMENT_NAME)
                    .toBundle();
            getActivity().startActivity(intent, bundle);
        }

        if (item instanceof PopcornShow) {
            PopcornShow show = (PopcornShow) item;
            Timber.d("Item: %s", item.toString());
            Intent intent = new Intent(getActivity(), ShowActivity.class);
            intent.putExtra(ShowActivity.SHOW, show);

            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),
                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                    ShowActivity.SHARED_ELEMENT_NAME)
                    .toBundle();
            getActivity().startActivity(intent, bundle);
        }
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        mRowsAdapter.clear();
        return true;
    }

    @SuppressLint("CheckResult")
    private void addMoviesSubscription(final MoviesAdapterPopcorn adapter, String query) {
        adapter.showLoadingIndicator();

        Map<String, String> options = adapter.getAdapterOptions();
        String nextPage = options.get(PopcornPaginationAdapter.KEY_NEXT_PAGE);
        String sort = options.get(PopcornPaginationAdapter.KEY_SORT);

        ApiCalls.getMoviesSearch(nextPage, sort, query)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(popcornMovies -> bindMoviesDetails(popcornMovies, adapter), Timber::e);
    }

    private void bindMoviesDetails(List<PopcornMovie> popcornMovies, MoviesAdapterPopcorn adapter) {
        adapter.removeLoadingIndicator();
        if (popcornMovies.size() == 0) {
            mRowsAdapter.remove(movies);
        } else {
            adapter.setNextPage();
            adapter.addAllItems(popcornMovies);
        }
    }

    @SuppressLint("CheckResult")
    private void addShowsubscription(final ShowsAdapterPopcorn adapter, String query) {
        adapter.showLoadingIndicator();

        Map<String, String> options = adapter.getAdapterOptions();
        String nextPage = options.get(PopcornPaginationAdapter.KEY_NEXT_PAGE);
        String sort = options.get(PopcornPaginationAdapter.KEY_SORT);

        ApiCalls.getShowsSearch(nextPage, sort, query)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(popcornShows -> bindShowDetails(popcornShows, adapter), Timber::e);
    }

    private void bindShowDetails(List<PopcornShow> popcornShows, ShowsAdapterPopcorn adapter) {
        adapter.removeLoadingIndicator();

        mRowsAdapter.remove(shows);

        adapter.addAllItems(popcornShows);

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mRowsAdapter.clear();

        if (TextUtils.isEmpty(query)) {
            return false;
        }

        movies = new MoviesAdapterPopcorn(getActivity(), "trending");
        mRowsAdapter.add(new ListRow(new HeaderItem(0, "Movies"), movies));

        shows = new ShowsAdapterPopcorn(getActivity(), "trending");
        mRowsAdapter.add(new ListRow(new HeaderItem(1, "Shows"), shows));

        addMoviesSubscription(movies, query);
        addShowsubscription(shows, query);

        return true;
    }
}