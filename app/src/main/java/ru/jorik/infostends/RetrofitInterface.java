package ru.jorik.infostends;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by 111 on 07.03.2017.
 */

public interface RetrofitInterface {

    @GET("/maps/api/directions/json")
    Call<RouteFromApi> postRoute(
            @Query(value = "origin") String origin,
            @Query(value = "destination") String destination,
            @Query("sensor") boolean hasSensor,
            @Query("language") String lang
    );


}
