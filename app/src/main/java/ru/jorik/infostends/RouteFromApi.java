package ru.jorik.infostends;

import java.util.List;

/**
 * Created by 111 on 15.03.2017.
 */

public class RouteFromApi {
    public List<Route> routes;

    public String getPoints(){
        return this.routes.get(0).overview_polyline.points;
    }



    class Route {
        OverviewPolyline overview_polyline;
    }



    class OverviewPolyline {
        String points;
    }

}
