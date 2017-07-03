package com.halangode.mapsnavigationapp.webservice.retrofit;

import com.halangode.mapsnavigationapp.webservice.bean.GoogleDirectionsResponse;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Created by Harikumar Alangode on 02-Jul-17.
 */

public class RetrofitInterface {
    public interface GoogleDirectionService{
        @GET("/maps/api/directions/{type}?")
        Call<GoogleDirectionsResponse> getDirections(@Path("type") String type, @QueryMap HashMap<String, String> query);
    }
}
