package ru.jorik.infostends;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by 111 on 13.04.2017.
 */

public class PolygonTag {
    private String name;
    private LatLng entryPoint, exitPoint;

    ///Constructions
    public PolygonTag(String name, LatLng entryPoint, LatLng exitPoint) {
        this.name = name;
        this.entryPoint = entryPoint;
        this.exitPoint = exitPoint;
    }

    ///Getters & Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LatLng getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(LatLng entryPoint) {
        this.entryPoint = entryPoint;
    }

    public LatLng getExitPoint() {
        return exitPoint;
    }

    public void setExitPoint(LatLng exitPoint) {
        this.exitPoint = exitPoint;
    }
}
