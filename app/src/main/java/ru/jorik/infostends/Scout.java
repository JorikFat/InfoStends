package ru.jorik.infostends;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;


public class Scout implements LocationListener {
    MainActivity mainActivity;
    LatLng exitPoint;

    public Scout(MainActivity mainActivity){
        this.mainActivity = mainActivity;
        exitPoint = mainActivity.polygonOut.getPosition();
    }


    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        String stringPosition = String.valueOf(lat) + " " + String.valueOf(lon);
        mainActivity.setMyPosition(location.getLatitude(), location.getLongitude());
        mainActivity.changeMyMarkerPosition();
        Toast.makeText(mainActivity, "Новая позиция " + stringPosition, Toast.LENGTH_SHORT).show();
        mainActivity.addCoordsInFinalPolygon(new LatLng(lat, lon));

        checkFinish();
        // TODO: 04.05.2017 проверить на достижение выходной точки
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    void checkFinish(){
        double distance = SphericalUtil.computeDistanceBetween(mainActivity.getMyPosition(), exitPoint);
        if (distance < Setting.RADIUS_IN_CHECKPOIT) {
            Toast.makeText(mainActivity, "Поздравляем", Toast.LENGTH_SHORT).show();
            Toast.makeText(mainActivity, "Вы закончили испытаение", Toast.LENGTH_SHORT).show();
        }
    }
}
