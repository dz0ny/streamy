package xyz.dz0ny.streamy.view;


import android.content.Context;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;
import xyz.dz0ny.streamy.BindableCardView;
import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.remote.popcorn.models.Episode;

public class EpisodeCardView extends BindableCardView<Episode> {

    @BindView(R.id.title)
    TextView mTitle;

    @BindView(R.id.aired)
    TextView mAired;

    @BindView(R.id.body)
    TextView mBody;

    public EpisodeCardView(Context context) {
        super(context);
        Timber.d("EpisodeCardView super");
        ButterKnife.bind(this);
    }

    @Override
    public void bind(Episode data) {
        mTitle.setText(data.getInfo());
        mBody.setText(data.getBody());
        mAired.setText(data.getAired());
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.card_episode;
    }
}