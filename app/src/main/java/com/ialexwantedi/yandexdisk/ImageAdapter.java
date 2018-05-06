package com.ialexwantedi.yandexdisk;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

/**
 * Класс адаптера для RecyclerView.
 */
public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ImageItem> imageList;
    private ClickListener clickListener = null;

    public ImageAdapter(List<ImageItem> list){
        this.imageList = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
        return new ImageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ImageViewHolder imageHolder = (ImageViewHolder) holder;
        ImageItem item = imageList.get(position);
        imageHolder.preview.setImageBitmap(item.getBitmap());
    }

    @Override
    public int getItemCount() { return imageList.size(); }
    public void setClickListener(ClickListener cl){ clickListener = cl; }

    class ImageViewHolder extends RecyclerView.ViewHolder{
        ImageView preview;

        private ImageViewHolder(View itemView) {
            super(itemView);

            preview = (ImageView) itemView.findViewById(R.id.preview);

            /* Добавление ClickListener'а RecyclerView */
            preview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(clickListener!=null){
                        clickListener.itemClicked(v, getAdapterPosition());
                    }
                }
            });
        }
    }
}
