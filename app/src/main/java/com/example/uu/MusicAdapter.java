package com.example.uu;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Created by U on 2018/4/4.
 */


public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder>{

    private Context mContext;
    private List<MusicMedia> musicList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    public MusicAdapter(List<MusicMedia> bookList, Context mContext){
        this.musicList = bookList;
        this.mContext = mContext;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_item,parent,false);
        final ViewHolder holder = new ViewHolder(view);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                if (mListener != null){
                    mListener.onItemClick(position);
                }
            }
        });
        return holder;
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        mListener = listener;
    }
    static class ViewHolder extends RecyclerView.ViewHolder{
        CardView cardView;
        ImageView image;
        TextView title;
        TextView singer;
        TextView duration;
        TextView size;
        public ViewHolder(View view) {
            super(view);
            cardView = (CardView) view;
            image = (ImageView) view.findViewById(R.id.video_imageView);
            title = (TextView) view.findViewById(R.id.video_title);
            singer = (TextView) view.findViewById(R.id.video_singer);
            size = (TextView) view.findViewById(R.id.video_size);
            duration = (TextView) view.findViewById(R.id.video_duration);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        MusicMedia music = musicList.get(position);
        Glide.with(mContext).load(R.drawable.hua).into(holder.image);
        holder.title.setText(music.getTitle()+"");
        holder.singer.setText(music.getArtist()+"");
        holder.size.setText(music.getSize()+"");
        holder.duration.setText(music.getTime()+"");
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }
}
