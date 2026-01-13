package com.nepal.iptv.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface IPTVService {
    @GET
    Call<String> getM3UPlaylist(@Url String url);
}
