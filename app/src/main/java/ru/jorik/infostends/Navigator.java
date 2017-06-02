package ru.jorik.infostends;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

/**
 * Created by 111 on 18.03.2017.
 */

public class Navigator implements LocationListener {

    private MainActivity mainActivity;

    int countRebuildWay = 0;
    final int maxCount = 5;
    double newDistance = 0;
    double oldDistance = 0;
    String resultCompare;

    int indexCheckpoint = 1;
    int indexFinishMarker;

    LatLng targetCoords;



    public Navigator (MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }


    @Override
    public void onLocationChanged(Location location) {
        /// заглушка
        mainActivity.setMyPosition(location.getLatitude(), location.getLongitude());

        if (mainActivity.checkNotNull(mainActivity.myPositionMarker)){
            mainActivity.changeMyMarkerPosition();
        } else {
            mainActivity.putMyPositionMarker();
        }


        //приближение и отдаление от пункта назначения
        if (mainActivity.checkNotNull(/*mainActivity.markersListWay,*/ mainActivity.route)){
            targetCoords = mainActivity.routeCoords.get(indexCheckpoint);
            indexFinishMarker = mainActivity.routeCoords.size()-1;
            // TODO: 06.05.2017  
            /*здесь есть баг:
            когда достигается точка checkpoint - дистанция до новой точки становится выше, чем 
            предыдущее до чекпоинта - и это воспринимается как отдаление от checkpoint'a. Нужно при 
            достижении checkpoint'а сбрасывать дистанции (еще не придумал правильный алгоритм для 
            этого)
             */
            if (newDistance == 0) {
                newDistance = mainActivity.calculateDistance(mainActivity.getMyPosition(), targetCoords);
                oldDistance = newDistance;
            } else if (oldDistance == 0){
                updateDistances();
            } else {
                updateDistances();
                if (mainActivity.compareDistance(newDistance, oldDistance) == 1) { //отдалился
                    distanceMore();
                } else if (mainActivity.compareDistance(newDistance, oldDistance) == -1) { //приблизился
                    distanceLess();
                } else { //не изменился
                    distanceUnchanged();
                }
                Toast.makeText(mainActivity, resultCompare, Toast.LENGTH_SHORT).show();
            }

            mainActivity.changeRouteFirstPoints();
            mainActivity.changePolyline();
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        /// заглушка
        Toast.makeText(mainActivity, "Статус изменен на " + s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderEnabled(String s) {
        /// заглушка
        Toast.makeText(mainActivity, "Провайдер доступен", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String s) {
        /// заглушка
        Toast.makeText(mainActivity, "Провайдер НЕ доступен", Toast.LENGTH_SHORT).show();
    }



    ///Utils
    private void updateDistances(){
        oldDistance = newDistance;
        newDistance = mainActivity.calculateDistance(mainActivity.getMyPosition(), targetCoords);
    }

    private void distanceMore(){
        resultCompare = "Отдалился" + countRebuildWay + "/" + maxCount;
        if (countRebuildWay > maxCount){
            countRebuildWay = 0;
            Toast.makeText(mainActivity, "Обновление маршрута", Toast.LENGTH_SHORT).show();
            // TODO: 03.04.2017 перестройка маршрута
            mainActivity.getNewRoute();
        } else {
            countRebuildWay++;
        }
    }

    private void distanceLess(){
        resultCompare = "Приблизился";
        countRebuildWay = 0;
        checkRadiusCheckpoint();
    }

    private void distanceUnchanged(){
        // TODO: 03.04.2017 заполнить
        resultCompare = "Не изменился";
    }

    private void checkRadiusCheckpoint(){
        double distance = SphericalUtil.computeDistanceBetween(mainActivity.getMyPosition(), targetCoords);
        if (distance < Setting.RADIUS_IN_CHECKPOIT){
            inCheckpoint();

            // TODO: 03.04.2017 изменить на нормальное условие
            if (indexCheckpoint == indexFinishMarker){
                resultCompare = "Вы прибыли";
                finishAchive();
            }
        }
    }

    private void inCheckpoint(){
        resultCompare = "CHECKPOIN";
        mainActivity.routeCoords.remove(1);
        mainActivity.changePolyline();
    }

    private void setNewCheckpoin(int index){
        // TODO: 03.04.2017 заполнить
        LatLng[] checkpointMass = new LatLng[1];
        LatLng to = checkpointMass[index];
    }

    void finishAchive(){
        // TODO: 06.05.2017 добавить очистку от других областей и заморозить входную/выходную точки
        mainActivity.initFinalRoute();
        mainActivity.changeLocationListener();
    }
}
