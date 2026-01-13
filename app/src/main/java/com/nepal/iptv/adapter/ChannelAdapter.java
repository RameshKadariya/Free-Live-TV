package com.nepal.iptv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.nepal.iptv.R;
import com.nepal.iptv.model.Channel;
import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {
    private List<Channel> channels = new ArrayList<>();
    private OnChannelClickListener listener;
    private OnFavoriteClickListener favoriteListener;

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Channel channel, int position);
    }

    public ChannelAdapter(OnChannelClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteListener = listener;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels != null ? channels : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public List<Channel> getChannels() {
        return channels;
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channels.get(position);
        holder.bind(channel, position);
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    class ChannelViewHolder extends RecyclerView.ViewHolder {
        private ImageView logoImageView;
        private TextView nameTextView;
        private TextView groupTextView;
        private ImageView favoriteIcon;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            logoImageView = itemView.findViewById(R.id.channelLogo);
            nameTextView = itemView.findViewById(R.id.channelName);
            groupTextView = itemView.findViewById(R.id.channelGroup);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onChannelClick(channels.get(position));
                }
            });
        }

        public void bind(Channel channel, int position) {
            nameTextView.setText(channel.getName());
            
            String groupInfo = "";
            if (channel.getGroup() != null && !channel.getGroup().isEmpty()) {
                groupInfo = channel.getGroup();
            }
            if (channel.getCountry() != null && !channel.getCountry().isEmpty()) {
                groupInfo += (groupInfo.isEmpty() ? "" : " • ") + channel.getCountry();
            }
            groupTextView.setText(groupInfo);
            groupTextView.setVisibility(groupInfo.isEmpty() ? View.GONE : View.VISIBLE);

            if (channel.getLogo() != null && !channel.getLogo().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(channel.getLogo())
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(logoImageView);
            } else {
                logoImageView.setImageResource(R.drawable.ic_launcher_foreground);
            }

            // Update favorite icon
            favoriteIcon.setImageResource(channel.isFavorite() ? 
                R.drawable.ic_favorite : R.drawable.ic_favorite_border);

            favoriteIcon.setOnClickListener(v -> {
                if (favoriteListener != null) {
                    favoriteListener.onFavoriteClick(channel, position);
                }
            });
        }
    }
}
