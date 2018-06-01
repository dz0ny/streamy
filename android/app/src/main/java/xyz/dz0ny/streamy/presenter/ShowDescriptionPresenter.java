package xyz.dz0ny.streamy.presenter;

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import xyz.dz0ny.streamy.remote.popcorn.models.ShowDetails;

public class ShowDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(AbstractDetailsDescriptionPresenter.ViewHolder viewHolder, Object item) {
        ShowDetails show = (ShowDetails) item;
        if (show != null) {
            viewHolder.getTitle().setText(show.getTitle());
            viewHolder.getSubtitle().setText(show.getInfo());
            viewHolder.getBody().setText(show.getBody());
        }
    }
}