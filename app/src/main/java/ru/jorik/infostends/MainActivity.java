package ru.jorik.infostends;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    //main
    GoogleMap googleMap;
    RetrofitInterface rInterface;
    LocationManager locationManager;
    LocationListener locationListener1 = new Navigator(this);

    Marker myPositionMarker;
    Marker polygonIn, polygonOut;
    private LatLng myPosition;

    List<PolygonOptions> polygonOptionsList = new ArrayList<PolygonOptions>();
    List<Polygon> polygonList = new ArrayList<Polygon>();
    List<PolygonTag> polygonTags = new ArrayList<PolygonTag>();
    Map<Polygon, PolygonTag> polygonMap = new HashMap<Polygon, PolygonTag>();
    Polygon selectedPolygon;
    Polygon finishSelectedPolygon;

    //route to:
    Polyline route;
    List<LatLng> routeCoords = new ArrayList<>();

    PolylineOptions asyncPolyline;
    MapFragment mapFragment;

    private LatLng enterPoint;
    private LatLng exitPoint;


    //route in:
    private List<LatLng> finalRouteInPolygon;



    ////Activity:
    /// Меню:
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.showAllPolygon:
                focusTo(getPoints(polygonMap.keySet()));
                break;
            case R.id.selectCancel:
                selectCancel();
                showAllPolygons();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    }
    ///---Меню



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment0);
        mapFragment.getMapAsync(this);

        initPolygonOptionsList();
        initStartLocation();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        checkGPSPermission();
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3, 5, locationListener1);
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3, 5, locationListener1);
        }



        //работа через AsyncTask
        //55.781254, 37.895539
        //55.773919, 37.895723
/*
        String responce = "";
        try {
            responce = new RouteBuilder().execute("55.781254,37.895539","55.773919,37.895723").get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        Log.i("respoce", responce);
*/

//        createMapView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Retrofit retrofit;
        retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        rInterface = retrofit.create(RetrofitInterface.class);
    }


    ////Implements
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;

        MapClicker mapClicker = new MapClicker();
        this.googleMap.setOnMapLongClickListener(mapClicker);
        this.googleMap.setOnMapClickListener(mapClicker);

        // TODO: 14.04.2017 проверка работы с полигонами
        this.googleMap.setOnPolygonClickListener(new PolygonShow());


                //область карты (возле Балашихи)
                //55.781012, 37.880669
                //55.781254, 37.895539
                //55.773919, 37.895723
                //55.774076, 37.881926

        showAllPolygons();

        focusTo(getPoints(polygonMap.keySet()));
    }


    ///Getters & Setters
    public void setMyPosition(double d1, double d2){
        myPosition = new LatLng(d1, d2);
    }

    public LatLng getMyPosition() {
        return myPosition;
    }


    //todo utils
    ///Utils
    public void getNewRoute(){
        routeCoords.clear();
        routeCoords.add(myPosition);
        route.remove();
        String from = String.valueOf(myPosition.latitude) + "," + String.valueOf(myPosition.longitude);
        LatLng llTo = ((PolygonTag) polygonMap.get(finishSelectedPolygon)).getEntryPoint();
        String to = String.valueOf(llTo.latitude) + "," + String.valueOf(llTo.longitude);
        // TODO: 14.05.2017 обвязка мега-костыля, для корректного вызова
        boolean b = false;
        addRoute(from, to, b);
    }

    public void changeLocationListener(){
        locationManager.removeUpdates(locationListener1);
        checkGPSPermission();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, new Scout(this));
    }

    public void initFinalRoute(){
        finalRouteInPolygon = new ArrayList<LatLng>();
    }

    public void addCoordsInFinalPolygon(LatLng coord){
        finalRouteInPolygon.add(coord);
    }

    void unselectPolygon(){
        if (checkNotNull(selectedPolygon)){
            selectedPolygon.setFillColor(Setting.ALL_COLOR_FILL);
            selectedPolygon.setStrokeColor(Setting.ALL_COLOR_LINE);
            selectedPolygon = null;
        }
    }

    void selectCancel(){
        // TODO: 26.04.2017 рефакторить
        finishSelectedPolygon = null;
        route.remove();
//        routeCoords = null;
        routeCoords.clear();

        polygonIn.remove();
        polygonIn = null;
        polygonOut.remove();
        polygonOut = null;
    }

    void changeRouteFirstPoints(){
        routeCoords.set(0, myPosition);
    }

    void changePolyline(){
        route.setPoints(routeCoords);
    }

    void addRoute(String from, String to){
        rInterface.postRoute(from, to, true, "ru").enqueue(new Callback<RouteFromApi>() {
            @Override
            public void onResponse(Call<RouteFromApi> call, Response<RouteFromApi> response) {
                List<LatLng> wayResponse = PolyUtil.decode(response.body().getPoints());
                routeCoords.add(myPosition);
                routeCoords.addAll(wayResponse);
                routeCoords.add(polygonMap.get(finishSelectedPolygon).getEntryPoint());

                addPolylineToMap(googleMap, wayResponse);


            }

            @Override
            public void onFailure(Call<RouteFromApi> call, Throwable t) {
                // TODO: 08.05.2017 Переписать заглушку
                Toast.makeText(MainActivity.this, "Ошибка построения маршрута", Toast.LENGTH_SHORT).show();
            }
        });

    }

    //todo убрать этот костылище
    /*
    метод сделан второпях, чтобы оставить первую точку маршрута и чтобы не вылетал iobe на changeRouteFirstPoints();
    Знаю, что поступил тут некрасиво. Но на меня напала муза, и надо творить до конца, сейчас 02:18 + я болею.
    Потом переделаю. Боюсь вдохновение пропадет.
     */
    void addRoute(String from, String to, boolean b){
        rInterface.postRoute(from, to, true, "ru").enqueue(new Callback<RouteFromApi>() {
            @Override
            public void onResponse(Call<RouteFromApi> call, Response<RouteFromApi> response) {
                List<LatLng> wayResponse = PolyUtil.decode(response.body().getPoints());
                routeCoords.addAll(wayResponse);
                routeCoords.add(polygonMap.get(finishSelectedPolygon).getEntryPoint());

                addPolylineToMap(googleMap, wayResponse);


            }

            @Override
            public void onFailure(Call<RouteFromApi> call, Throwable t) {
                // TODO: 08.05.2017 Переписать заглушку
                Toast.makeText(MainActivity.this, "Ошибка построения маршрута", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initPolygonOptionsList(){
        List<LatLng> pointsList;
        PolygonOptions tempPolygon;
        LatLng enterPoint, exitPoint;



        // TODO: 13.04.2017 переделать метод, для заполнения данными из ресурсов
        //polygon1 Озерная
        String[] tempPolygonArrays = getResources().getStringArray(R.array.polygons);
        String[] polygonArrays = new String[tempPolygonArrays.length];
        for (int i=0; i<tempPolygonArrays.length; i++){
            polygonArrays[i] = tempPolygonArrays[i].substring(2);
        }

        for (int i1=0; i1<polygonArrays.length; i1++){

            pointsList = new ArrayList<LatLng>();
            tempPolygon = new PolygonOptions();

            String tempPolygonString = polygonArrays[i1];
            List<String> tempList = new ArrayList();
            for (int i=0; i<tempPolygonString.length()-1; i+=21){
                tempList.add(tempPolygonString.substring(i, i + 20));
            }
            String[] coordsArray = tempList.toArray(new String[tempList.size()]);

            for (int i = 1; i<coordsArray.length-1; i++){
                pointsList.add(fromStringToLatLng(coordsArray[i]));
            }

            addPolygonPoints(tempPolygon, pointsList.toArray(new LatLng[pointsList.size()]));
            polygonOptionsList.add(tempPolygon);
            enterPoint = fromStringToLatLng(coordsArray[0]);
            exitPoint = fromStringToLatLng(coordsArray[coordsArray.length-1]);
            //// TODO: 28.04.2017 Доработать таг имени
            polygonTags.add(new PolygonTag("Озерная", enterPoint, exitPoint));
//            edgePoints.add(new LatLng[]{enterPoint, exitPoint});

        }
//        String[] lll = lllm[0];
//        LatLng[] lll1 = new LatLng[6];
//        String[] tempStrings = lll[0].split(", ");;
//        Double tempLat = Double.parseDouble(tempStrings[0]);
//        Double tempLng = Double.parseDouble(tempStrings[1]);
//        lll1[0] = new LatLng(tempLat, tempLng);

//        String[] erPtSt = lll[0].split(", ");
//        tempLat = Double.parseDouble(erPtSt[0]);
//        tempLng = Double.parseDouble(erPtSt[1]);





//        pointsList = new ArrayList<LatLng>();
//        pointsList.add(new LatLng(54.323567, 40.865452));
//        pointsList.add(new LatLng(54.324621, 40.871846));
//        pointsList.add(new LatLng(54.322055, 40.872979));
//        pointsList.add(new LatLng(54.320751, 40.867207));
//        tempPolygon = new PolygonOptions();
//        addPolygonPoints(tempPolygon, pointsList.toArray(new LatLng[pointsList.size()]));
//        polygonOptionsList.add(tempPolygon);

//        enterPoint = new LatLng(54.323134, 40.872357);
//        exitPoint = new LatLng(54.322394, 40.866241);
//        polygonTags.add(new PolygonTag("Озерная", enterPoint, exitPoint));
//        edgePoints.add(new LatLng[]{enterPoint, exitPoint});

/*
        //polygon2 ЦРБ
        pointsList = new ArrayList<LatLng>();
        pointsList.add(new LatLng(54.325153, 40.887205));
        pointsList.add(new LatLng(54.323259, 40.887034));
        pointsList.add(new LatLng(54.323979, 40.892278));
        pointsList.add(new LatLng(54.326420, 40.890918));
        tempPolygon = new PolygonOptions();
        addPolygonPoints(tempPolygon, pointsList.toArray(new LatLng[pointsList.size()]));
        polygonOptionsList.add(tempPolygon);

        enterPoint = new LatLng(54.323667, 40.889233);
        exitPoint = new LatLng(54.326249, 40.888115);
        polygonTags.add(new PolygonTag("ЦРБ", enterPoint, exitPoint));
        edgePoints.add(new LatLng[]{enterPoint, exitPoint});


        //polygon3 РоссельхозБанк
        pointsList = new ArrayList<LatLng>();
        pointsList.add(new LatLng(54.322453, 40.877335));
        pointsList.add(new LatLng(54.321327, 40.877067));
        pointsList.add(new LatLng(54.321283, 40.878719));
        pointsList.add(new LatLng(54.322522, 40.878622));
        tempPolygon = new PolygonOptions();
        addPolygonPoints(tempPolygon, pointsList.toArray(new LatLng[pointsList.size()]));
        polygonOptionsList.add(tempPolygon);

        enterPoint = new LatLng(54.322454, 40.878361);
        exitPoint = new LatLng(54.321462, 40.878098);
        polygonTags.add(new PolygonTag("РоссельхозБанк", enterPoint, exitPoint));
        edgePoints.add(new LatLng[]{enterPoint, exitPoint});


*/
        // add more polygons
    }

    LatLng fromStringToLatLng(String coords){
        String[] doubles = coords.split(", ");
        Double tempLat = Double.parseDouble(doubles[0]);
        Double tempLng = Double.parseDouble(doubles[1]);
        return new LatLng(tempLat, tempLng);
    }

    private void initEdgePoint(){

    }
/*
    private void setMarkersWay(List<LatLng> coords) {
        int length = coords.size();
        markersListWay = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            String title = String.valueOf(i);
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
//            if (i == length - 1){
//                title = "finish";
//                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
//            }
            markersListWay.add(googleMap.addMarker(new MarkerOptions()
                    .position(coords.get(i))
                    .title(title)
                    .icon(icon)
            )
            );
//            markersWay[i] = googleMap.addMarker(new MarkerOptions()
//                    .position(coords.get(i))
//                    .title(title)
//                    .icon(icon)
//            );
        }
    }
*/
/*
    private void showPopupMenu(final LatLng latLng){
        View v = findViewById(R.id.fragment0);
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.map_long_menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.mlm1:
                        addFinishMarker(latLng);
                        Toast.makeText(MainActivity.this, "add Finish Marker", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.mlm2:
                        Toast.makeText(MainActivity.this, "add temp Marker N " + addTempMarker(latLng),
                                Toast.LENGTH_SHORT).show();
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }
*/

/*
    private void addFinishMarker(LatLng position){
        if (googleMap != null){
            finishMarker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Finish Marker")
                    .draggable(false)
            );
        }
    }
*/

    /*
    * Вычисление дистанции
    */
    double calculateDistance(LatLng l1, LatLng l2){
        double x1, y1;
        double x2, y2;
        double result;

        x1 = l1.latitude;
        y1 = l1.longitude;
        x2 = l2.latitude;
        y2 = l2.longitude;

        double tempX = x2-x1;
        double tempY = y2-y1;

        result = Math.sqrt((tempX * tempX) + (tempY * tempY));

        return result;
    }

    /*
    *первая сравнивается со второй
    * d1 > d2  output 1
    * d1 < d2  output -1
    * d1 = d2  output 0
    */
    int compareDistance(Double d1, Double d2){
        if (d1>d2){
            return 1;
        }else if (d1<d2){
            return -1;
        }
        else return 0;
    }

    public void showAllPolygons(){
        for (int i = 0; i<polygonOptionsList.size(); i++){
            Polygon p = googleMap.addPolygon(polygonOptionsList.get(i));
            p.setClickable(true);
            polygonList.add(p);


            //// TODO: 13.04.2017 убрать проверку
            polygonMap.put(p, polygonTags.get(i));
        }
    }

    private void addMarker(LatLng position){
        if(googleMap != null){
            googleMap.addMarker(new MarkerOptions()
            .position(position)
            .title("new Marker")
            .draggable(true));
        }
    }

/*
    // TODO: 03.04.2017 удалить после проверок
    private int addTempMarker(LatLng position){
        int lim = markers.length;
        for (int i=0; i<lim; i++){
            if (markers[i] == null){
                String title = String.valueOf(i);
                BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                if (i == lim-1){
                    title = "finish";
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
                }
                markers[i] = googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(title)
                        .draggable(false)
                        .icon(icon)
                );
                return i;
            }
        }
        return -1;
    }
*/

    private void addPolygonPoints (PolygonOptions polygon, LatLng[] points){
        polygon.strokeColor(Setting.ALL_COLOR_LINE)
                .strokeWidth(Setting.ALL_WIDTH_LINE)
                .fillColor(Setting.ALL_COLOR_FILL);
        for (LatLng ll : points){
            polygon.add(ll);
        }
    }

    private void addPolylineToMap(GoogleMap map, List<LatLng> points){
        if (myPositionMarker == null){
            Toast.makeText(this, "Определяется Ваше метонахождение", Toast.LENGTH_SHORT).show();
        } else {
            PolylineOptions way = new PolylineOptions()
                    .width(Setting.WAY_WIDTH_LINE)
                    .color(Setting.WAY_COLOR_LINE);
            //заполнение точками маршрута
            way.add(myPosition);
            for(LatLng ll : points){
                way.add(ll);
            }
            way.add(((PolygonTag) polygonMap.get(finishSelectedPolygon)).getEntryPoint());

            route = map.addPolyline(way);
        }
    }

    private void checkGPSPermission(){
        String permission1 = Manifest.permission.ACCESS_FINE_LOCATION;
        String permission2 = Manifest.permission.ACCESS_COARSE_LOCATION;
        int p = PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(permission1) != p && this.checkSelfPermission(permission2) != p){
                Toast.makeText(this, "Включите GPS", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initStartLocation(){
        checkGPSPermission();

        LocationManager lm  = (LocationManager) getSystemService(LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);
        Location bl = null;
        for (String p : providers){
            Location l = lm.getLastKnownLocation(p);
            if (l == null){
                continue;
            }
            if (bl == null || l.getAccuracy() < bl.getAccuracy()) {
                bl = l;
            }
        }
        if (checkNotNull(bl)){
            setMyPosition(bl.getLatitude(), bl.getLongitude());
        }
    }

    //Универсальная проверка на null
    boolean checkNotNull (Object... objects){
        for (Object o : objects){
            if (o == null) return false;
        }
        return true;
    }

    private List<LatLng> getPoints(Set<Polygon> polygons){
        List<LatLng> returnList = new ArrayList<>();
        for (Polygon p : polygons){
            returnList.addAll(p.getPoints());
        }
        return returnList;
    }


    private void focusToAll(){
        LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();
        Set<Polygon> polygonList = polygonMap.keySet();
        for (Polygon po : polygonList){
            List<LatLng> lll = po.getPoints();
            for (LatLng ll : lll){
                latLngBoundsBuilder.include(ll);
            }
        }
        LatLngBounds bounds = latLngBoundsBuilder.build();
        int boundSize = getResources().getDisplayMetrics().widthPixels; //ширина дисплея
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, boundSize, boundSize, 25);
        googleMap.moveCamera(cameraUpdate);
    }

/*
    private void focusTo(Polygon... polygons){
        LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();
        for (Polygon po : polygons){
            List<LatLng> lll = po.getPoints();
            for (LatLng ll : lll){
                latLngBoundsBuilder.include(ll);
            }
        }
        LatLngBounds bounds = latLngBoundsBuilder.build();
        int boundSize = getResources().getDisplayMetrics().widthPixels; //ширина дисплея
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, boundSize, boundSize, 15);
        googleMap.moveCamera(cameraUpdate);
    }
*/

    private void focusTo(List<LatLng> coords){
        LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();
        for (LatLng ll : coords){
            latLngBoundsBuilder.include(ll);
        }
        LatLngBounds bounds = latLngBoundsBuilder.build();
        int boundSize = getResources().getDisplayMetrics().widthPixels; //ширина дисплея
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, boundSize, boundSize, 15);
        googleMap.moveCamera(cameraUpdate);
    }



    //public methods:
    public void putMyPositionMarker(){
        if (checkNotNull(googleMap, myPosition)){
            MarkerOptions tempMarker = new MarkerOptions()
                    .position(myPosition)
                    .title("Я")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    );

            myPositionMarker = googleMap.addMarker(tempMarker);
        }
    }

    public void changeMyMarkerPosition(){
        if (checkNotNull(myPositionMarker)) {
            myPositionMarker.setPosition(myPosition);
        }
    }

    public void clearMap(){
        Set<Polygon> tempSet = polygonMap.keySet();
        for (Polygon p : tempSet){
            if (!p.equals(finishSelectedPolygon)){
                p.remove();
            }
        }
    }



    class PolygonShow implements GoogleMap.OnPolygonClickListener{
        @Override // TODO: 14.04.2017 Добавить возможность выбора Полигона.
        public void onPolygonClick(Polygon polygon) {
            PolygonTag pt = (PolygonTag) polygonMap.get(polygon);
            String temtpText = pt.getName();
            Toast.makeText(MainActivity.this, temtpText, Toast.LENGTH_SHORT).show();

            selectPolygon(polygon);
        }

        // TODO: 15.04.2017 Рефакторить
        // TODO: 15.04.2017 Добавить Контекстное меню
        private void selectPolygon(Polygon p){
            if (polygonIn == null && polygonOut == null){
                PolygonTag pt = (PolygonTag) polygonMap.get(p);


                BitmapDescriptor iconIn = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
                BitmapDescriptor iconOut = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);

                MarkerOptions pIn, pOut;
                pIn = new MarkerOptions()
                        .position(pt.getEntryPoint())
                        .title("Вход")
                        .icon(iconIn);
                pOut = new MarkerOptions()
                        .position(pt.getExitPoint())
                        .title("Выход")
                        .icon(iconOut);

                polygonIn = googleMap.addMarker(pIn);
                polygonOut = googleMap.addMarker(pOut);

            } else {
                PolygonTag pt = (PolygonTag) polygonMap.get(p);
                polygonIn.setPosition(pt.getEntryPoint());
                polygonOut.setPosition(pt.getExitPoint());
            }

            unselectPolygon();
/*
            for (Polygon p1 : polygonMap.keySet()){
                p1.setFillColor(Setting.ALL_COLOR_FILL);
                p1.setStrokeColor(Setting.ALL_COLOR_LINE);
            }
*/

            p.setFillColor(Setting.SEL_COLOR_FILL);
            p.setStrokeColor(Setting.SEL_COLOR_LINE);

            focusTo(p.getPoints());

            selectedPolygon = p;
        }
    }



    private class MapClicker implements GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener{

        @Override
        public void onMapLongClick(LatLng latLng) {

            if (selectedPolygon == null){
                Toast.makeText(MainActivity.this, "Выберите область", Toast.LENGTH_SHORT).show();
            } else if(finishSelectedPolygon != null){
                String toastText = "Выбрана область \"" + polygonMap.get(finishSelectedPolygon).getName() + "\"";
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
            } else {
                finishSelectedPolygon = selectedPolygon;
                selectedPolygon = null;
                String tempString = "Выбранна область \"" + polygonMap.get(finishSelectedPolygon).getName() + "\"";
                Toast.makeText(MainActivity.this, tempString, Toast.LENGTH_SHORT).show();

                clearMap();

                //построение маршрута
                if (checkNotNull(myPosition)) {
                    String from = String.valueOf(myPosition.latitude) + "," + String.valueOf(myPosition.longitude);
                    LatLng llTo = ((PolygonTag) polygonMap.get(finishSelectedPolygon)).getEntryPoint();
                    String to = String.valueOf(llTo.latitude) + "," + String.valueOf(llTo.longitude);
                    addRoute(from, to);
                }
            }

        }

        @Override
        public void onMapClick(LatLng latLng) {
            if (checkNotNull(selectedPolygon)) {
                unselectPolygon();
            }
        }

        private void unselectPolygon(){
            MainActivity.this.unselectPolygon();
            polygonIn.remove();
            polygonIn = null;
            polygonOut.remove();
            polygonOut = null;
        }
    }

}

