package com.halangode.mapsnavigationapp.webservice.retrofit;

/**
 * Created by Harikumar Alangode on 02-Jul-17.
 */

public class ApiUtils {

    private static final String BASE_URL = "https://maps.googleapis.com/";

    public static RetrofitInterface.GoogleDirectionService getDirectionGoogle() {
        return RetrofitClient.getClient(BASE_URL).create(RetrofitInterface.GoogleDirectionService.class);
    }
}
