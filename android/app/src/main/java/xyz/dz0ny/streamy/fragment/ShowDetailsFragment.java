package xyz.dz0ny.streamy.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.app.DetailsFragmentBackgroundController;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v4.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.activity.DetailsActivity;
import xyz.dz0ny.streamy.activity.MainActivity;
import xyz.dz0ny.streamy.activity.ShowActivity;
import xyz.dz0ny.streamy.presenter.EpisodePresenter;
import xyz.dz0ny.streamy.presenter.ShowDescriptionPresenter;
import xyz.dz0ny.streamy.remote.ApiCalls;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornShow;
import xyz.dz0ny.streamy.remote.popcorn.models.ShowDetails;

public class ShowDetailsFragment extends DetailsFragment {

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 413;

    private ArrayObjectAdapter mAdapter;
    private DetailsFragmentBackgroundController mDetailsBackground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timber.i("onCreate");
        super.onCreate(savedInstanceState);

        mDetailsBackground = new DetailsFragmentBackgroundController(this);

        PopcornShow show = (PopcornShow) getActivity().getIntent().getSerializableExtra(ShowActivity.SHOW);
        if (show != null) {
            // Set detail background.
            FullWidthDetailsOverviewRowPresenter detailsPresenter =
                    new FullWidthDetailsOverviewRowPresenter(new ShowDescriptionPresenter());
            detailsPresenter.setBackgroundColor(
                    ContextCompat.getColor(getContext(), R.color.selected_background));

            // Hook up transition element.
            FullWidthDetailsOverviewSharedElementHelper sharedElementHelper =
                    new FullWidthDetailsOverviewSharedElementHelper();
            sharedElementHelper.setSharedElementEnterTransition(
                    getActivity(), DetailsActivity.SHARED_ELEMENT_NAME);
            detailsPresenter.setListener(sharedElementHelper);
            detailsPresenter.setParticipatingEntranceTransition(true);


            ClassPresenterSelector mPresenterSelector = new ClassPresenterSelector();
            mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
            mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
            mAdapter = new ArrayObjectAdapter(mPresenterSelector);
            setAdapter(mAdapter);

            fetchShowDetails(show);

        } else {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

    private void initializeBackground(ShowDetails data) {
        mDetailsBackground.enableParallax();
        Glide.with(getActivity())
                .load(data.getImages().getBanner())
                .asBitmap()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.default_background)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                                                GlideAnimation<? super Bitmap> glideAnimation) {
                        mDetailsBackground.setCoverBitmap(bitmap);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });
    }

    private void setupDetailsOverviewRow(ShowDetails show) {
        Timber.i("doInBackground: %s", show.toString());
        final DetailsOverviewRow row = new DetailsOverviewRow(show);
        row.setImageDrawable(
                ContextCompat.getDrawable(getContext(), R.drawable.default_background));
        int width = convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH);
        int height = convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT);
        Glide.with(getActivity())
                .load(show.getImages().getPoster())
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.default_background)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        Timber.i("details overview card image url ready: %s", resource);
                        row.setImageDrawable(resource);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });
        mAdapter.add(row);
    }

    private int convertDpToPixel(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @SuppressLint("CheckResult")
    private void fetchShowDetails(PopcornShow show) {
        Timber.i("Show id %s", show.getID());
        ApiCalls.getShow(show.getID())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(this::bindShowDetails, Timber::e);
    }

    @SuppressLint("DefaultLocale")
    private void bindShowDetails(ShowDetails response) {
        ShowDetails showDetails = response;
        Timber.i("bindShowDetails");
        Timber.i(String.valueOf(response));

        setupDetailsOverviewRow(showDetails);
        initializeBackground(showDetails);

        for (int i = 1; i <= response.getSeasons(); i++) {
            String season = String.format("Season %d", i);
            ArrayObjectAdapter showsAdapter = new ArrayObjectAdapter(new EpisodePresenter());
            showsAdapter.addAll(0, response.getEpisodes(i));
            mAdapter.add(new ListRow(new HeaderItem(i - 1, season), showsAdapter));
        }

    }
}