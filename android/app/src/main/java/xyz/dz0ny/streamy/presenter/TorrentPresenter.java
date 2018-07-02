package xyz.dz0ny.streamy.presenter;


import android.content.Context;
import android.support.v17.leanback.widget.Presenter;
import android.support.v7.view.ContextThemeWrapper;
import android.view.ViewGroup;

import timber.log.Timber;
import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.remote.streamy.models.StreamyTorrent;
import xyz.dz0ny.streamy.view.TorrentCardView;

public class TorrentPresenter extends Presenter {

    Context mContext;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {

        if (mContext == null) {
            // We do this to avoid creating a new ContextThemeWrapper for each one of the cards
            // If we look inside the ImageCardView they warn us about the same this.
            // Example: Try using the constructor: ImageCardView(context, style)
            // It is deprecated right? This is because that constructor creates a new ContextThemeWrapper every time a
            // ImageCardView is allocated.
            mContext = new ContextThemeWrapper(parent.getContext(), R.style.AppTheme);
        }
        Timber.d("onCreateViewHolder Episode");
        return new ViewHolder(new TorrentCardView(mContext));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Timber.d("onBindViewHolder Torrents");
        ((TorrentCardView) viewHolder.view).bind((StreamyTorrent) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
