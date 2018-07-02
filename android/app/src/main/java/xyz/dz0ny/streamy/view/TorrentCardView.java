package xyz.dz0ny.streamy.view;

import android.content.Context;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import xyz.dz0ny.streamy.BindableCardView;
import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.remote.popcorn.models.Episode;
import xyz.dz0ny.streamy.remote.streamy.models.StreamyTorrent;

public class TorrentCardView extends BindableCardView<Episode> {

    @BindView(R.id.title)
    TextView mTitle;

    @BindView(R.id.aired)
    TextView mAired;

    @BindView(R.id.body)
    TextView mBody;

    public TorrentCardView(Context context) {
        super(context);
        ButterKnife.bind(this);
    }

    @Override
    public void bind(Object data) {
        StreamyTorrent ep = (StreamyTorrent) data;
        mTitle.setText(ep.getName());
        mBody.setText(ep.getHash());
        mAired.setText(String.format("%s/%s", ep.getSize(), ep.getDownloaded()));
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.card_torrent;
    }
}