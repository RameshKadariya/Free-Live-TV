package com.nepal.iptv.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.nepal.iptv.model.Channel;
import com.nepal.iptv.repository.ChannelRepository;
import java.util.List;

public class ChannelViewModel extends ViewModel {
    private ChannelRepository repository;
    private LiveData<List<Channel>> channelsLiveData;

    public ChannelViewModel() {
        repository = ChannelRepository.getInstance();
    }

    public LiveData<List<Channel>> getChannels(String m3uUrl) {
        if (channelsLiveData == null) {
            channelsLiveData = repository.loadChannels(m3uUrl);
        }
        return channelsLiveData;
    }

    public void refreshChannels(String m3uUrl) {
        channelsLiveData = repository.loadChannels(m3uUrl);
    }
}
