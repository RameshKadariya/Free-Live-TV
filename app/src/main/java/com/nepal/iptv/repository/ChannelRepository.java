package com.nepal.iptv.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.nepal.iptv.model.Channel;
import com.nepal.iptv.network.IPTVService;
import com.nepal.iptv.network.RetrofitClient;
import com.nepal.iptv.parser.M3UParser;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChannelRepository {
    private static ChannelRepository instance;
    private IPTVService iptvService;

    private ChannelRepository() {
        iptvService = RetrofitClient.getIPTVService();
    }

    public static synchronized ChannelRepository getInstance() {
        if (instance == null) {
            instance = new ChannelRepository();
        }
        return instance;
    }

    public LiveData<List<Channel>> loadChannels(String m3uUrl) {
        MutableLiveData<List<Channel>> channelsLiveData = new MutableLiveData<>();

        iptvService.getM3UPlaylist(m3uUrl).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Channel> channels = M3UParser.parse(response.body());
                    channelsLiveData.postValue(channels);
                } else {
                    channelsLiveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                channelsLiveData.postValue(null);
            }
        });

        return channelsLiveData;
    }
}
