/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package xyz.dz0ny.streamy.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.app.BrowseSupportFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.activity.DetailsActivity;
import xyz.dz0ny.streamy.activity.ShowActivity;
import xyz.dz0ny.streamy.adapter.MoviesAdapterPopcorn;
import xyz.dz0ny.streamy.adapter.PopcornPaginationAdapter;
import xyz.dz0ny.streamy.adapter.ShowsAdapterPopcorn;
import xyz.dz0ny.streamy.remote.ApiCalls;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornMovie;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornShow;

public class MainFragment extends BrowseSupportFragment {

    private static final int BACKGROUND_UPDATE_DELAY = 300;

    private final Handler mHandler = new Handler();
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;
    private ArrayObjectAdapter rowsAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Timber.i("onCreate");
        super.onActivityCreated(savedInstanceState);

        prepareBackgroundManager();
        setupUIElements();
        loadRows();
        setupEventListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Timber.d("onDestroy: %s", mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    private void loadRows() {
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        MoviesAdapterPopcorn popular = new MoviesAdapterPopcorn(getActivity(), "trending");
        addMoviesSubscription(popular);
        rowsAdapter.add(new ListRow(new HeaderItem(0, "Popular Movies"), popular));
        MoviesAdapterPopcorn newest = new MoviesAdapterPopcorn(getActivity(), "last added");
        addMoviesSubscription(newest);
        rowsAdapter.add(new ListRow(new HeaderItem(1, "Newest Movies"), newest));
        MoviesAdapterPopcorn rated = new MoviesAdapterPopcorn(getActivity(), "rating");
        addMoviesSubscription(rated);
        rowsAdapter.add(new ListRow(new HeaderItem(2, "Movies by Rating"), rated));


        ShowsAdapterPopcorn populars = new ShowsAdapterPopcorn(getActivity(), "trending");
        addShowsubscription(populars);
        rowsAdapter.add(new ListRow(new HeaderItem(3, "Popular Shows"), populars));
        ShowsAdapterPopcorn newests = new ShowsAdapterPopcorn(getActivity(), "updated");
        addShowsubscription(newests);
        rowsAdapter.add(new ListRow(new HeaderItem(4, "Newest Shows"), newests));
        ShowsAdapterPopcorn rateds = new ShowsAdapterPopcorn(getActivity(), "rating");
        addShowsubscription(rateds);
        rowsAdapter.add(new ListRow(new HeaderItem(5, "Shows by Rating"), rateds));

        setAdapter(rowsAdapter);
    }

    @SuppressLint("CheckResult")
    private void addShowsubscription(final ShowsAdapterPopcorn adapter) {
        if (adapter.shouldShowLoadingIndicator()) adapter.showLoadingIndicator();

        Map<String, String> options = adapter.getAdapterOptions();
        String nextPage = options.get(PopcornPaginationAdapter.KEY_NEXT_PAGE);
        String sort = options.get(PopcornPaginationAdapter.KEY_SORT);

        ApiCalls.getShows(nextPage, sort)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(popcornShows -> bindShowDetails(popcornShows, adapter), Timber::e);
    }

    @SuppressLint("CheckResult")
    private void addMoviesSubscription(final MoviesAdapterPopcorn adapter) {
        if (adapter.shouldShowLoadingIndicator()) adapter.showLoadingIndicator();

        Map<String, String> options = adapter.getAdapterOptions();
        String nextPage = options.get(PopcornPaginationAdapter.KEY_NEXT_PAGE);
        String sort = options.get(PopcornPaginationAdapter.KEY_SORT);

        ApiCalls.getMovies(nextPage, sort)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(popcornMovies -> bindMoviesDetails(popcornMovies, adapter), Timber::e);

    }

    private void bindMoviesDetails(List<PopcornMovie> popcornMovies, MoviesAdapterPopcorn adapter) {
        adapter.removeLoadingIndicator();
        if (adapter.size() == 0 && popcornMovies.isEmpty()) {
            adapter.showReloadCard();
        } else {
            adapter.setNextPage();
            adapter.addAllItems(popcornMovies);
        }
    }

    private void bindShowDetails(List<PopcornShow> popcornShows, ShowsAdapterPopcorn adapter) {
        adapter.removeLoadingIndicator();
        if (adapter.size() == 0 && popcornShows.isEmpty()) {
            adapter.showReloadCard();
        } else {
            adapter.setNextPage();
            adapter.addAllItems(popcornShows);
        }
    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());

        mDefaultBackground = ContextCompat.getDrawable(getContext(), R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getContext(), R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(ContextCompat.getColor(getContext(), R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(view ->
                Toast.makeText(getActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
                        .show());

        setOnItemViewClickedListener(this::getOnItemViewClickedListener);
        setOnItemViewSelectedListener(this::getOnItemViewSelectedListener);
    }

    private void getOnItemViewSelectedListener(
        Presenter.ViewHolder itemViewHolder,
        Object item,
        RowPresenter.ViewHolder rowViewHolder,
        Row row
    ) {
        if (item instanceof PopcornMovie) {
            mBackgroundUri = ((PopcornMovie) item).getImages().getFanart();
            startBackgroundTimer();

            int index = rowsAdapter.indexOf(row);
            MoviesAdapterPopcorn adapter =
                    ((MoviesAdapterPopcorn) ((ListRow) rowsAdapter.get(index)).getAdapter());
            if (adapter.get(adapter.size() - 1).equals(item) && adapter.shouldLoadNextPage()) {
                addMoviesSubscription(adapter);
            }
        }
        if (item instanceof PopcornShow) {
            mBackgroundUri = ((PopcornShow) item).getImages().getFanart();
            startBackgroundTimer();

            int index = rowsAdapter.indexOf(row);
            ShowsAdapterPopcorn adapter =
                    ((ShowsAdapterPopcorn) ((ListRow) rowsAdapter.get(index)).getAdapter());
            if (adapter.get(adapter.size() - 1).equals(item) && adapter.shouldLoadNextPage()) {
                addShowsubscription(adapter);
            }
        }
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

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(mDefaultBackground)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        mBackgroundManager.setDrawable(resource);
                    }
                });
        mBackgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(() -> updateBackground(mBackgroundUri));
        }
    }

}
