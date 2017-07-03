package com.halangode.mapsnavigationapp.ui.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.halangode.mapsnavigationapp.R;
import com.halangode.mapsnavigationapp.util.DataParser;
import com.halangode.mapsnavigationapp.webservice.bean.GoogleDirectionsResponse;
import com.halangode.mapsnavigationapp.webservice.retrofit.ApiUtils;
import com.halangode.mapsnavigationapp.webservice.retrofit.RetrofitInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener, GoogleMap.OnPolylineClickListener {

    //constants
    private final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private final int PLACE_AUTOCOMPLETE_REQUEST_CODE_SOURCE = 2;
    private final int PLACE_AUTOCOMPLETE_REQUEST_CODE_DESTINATION = 3;


    //other
    private boolean isLocationGranted;
    private String TAG = this.getClass().getSimpleName();

    //views
    private EditText sourceET;
    private EditText destinationET;
    private TextView directionsTV;
    private Toolbar toolbar;
    private NestedScrollView bottomSheet;
    private ProgressDialog progressDialog;

    //map stuff
    private Polyline[] polylines;
    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private Marker mCurrLocationMarker;
    private Marker sourceMarker;
    private Marker destinationMarker;
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if(mCurrLocationMarker != null){
                mCurrLocationMarker.remove();
            }
            mCurrLocationMarker = setMarker(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude(), "Current Location", BitmapDescriptorFactory.HUE_GREEN);
            stopLocationUpdates();
        }

        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);
        bottomSheet = (NestedScrollView) findViewById(R.id.bottom_sheet);
        progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
        mapFragment.getMapAsync(this);

        sourceET = (EditText) findViewById(R.id.sourceET);
        sourceET.setOnClickListener(this);
        destinationET = (EditText) findViewById(R.id.destinationET);
        destinationET.setOnClickListener(this);
        directionsTV = (TextView) findViewById(R.id.directionsTV);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkPermission();
        createLocationRequest();
    }

    private void fireIntent(int flag){
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                    .build(this);
            startActivityForResult(intent, flag);
        } catch (GooglePlayServicesRepairableException e) {

        } catch (GooglePlayServicesNotAvailableException e) {

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkPermission();
        if(isLocationGranted){
            startLocationUpdates();
            zoomCamera();
        }
    }

    private void startLocationUpdates() {
        checkPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            isLocationGranted = false;

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
        else{
            isLocationGranted = true;
        }

    }

    private void clearPolyline(){
        if(polylines != null){
            for(Polyline p : polylines){
                if(p != null){
                    p.remove();
                }
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isLocationGranted = true;
                    startLocationUpdates();
                } else {
                    isLocationGranted = false;
                }
            }
        }
    }

    private void fetchDirection(String originLat, String originLon, String desLat, String desLon) {
        RetrofitInterface.GoogleDirectionService directionService = ApiUtils.getDirectionGoogle();

        HashMap<String, String> query = new HashMap<>();
        query.put("alternatives", "true");
        query.put("origin", String.valueOf(originLat + "," + originLon));
        query.put("destination", String.valueOf(desLat + "," + desLon));


        directionService.getDirections("json", query).enqueue(new Callback<GoogleDirectionsResponse>() {
            @Override
            public void onResponse(Call<GoogleDirectionsResponse> call, Response<GoogleDirectionsResponse> response) {
                getParsedDataObservable(response.body()).subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(getParsedDataObserver());
            }

            @Override
            public void onFailure(Call<GoogleDirectionsResponse> call, Throwable t) {
                if(progressDialog.isShowing()){
                    progressDialog.dismiss();
                }
            }
        });
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    private Marker setMarker(double lat, double lon, String s, float color){
        if(mMap != null){
            LatLng latLng = new LatLng(lat, lon);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(s);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(color));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,11));
            return  mMap.addMarker(markerOptions.title(s));
        }
        return null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnPolylineClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.sourceET:
                fireIntent(PLACE_AUTOCOMPLETE_REQUEST_CODE_SOURCE);
                break;
            case R.id.destinationET:
                fireIntent(PLACE_AUTOCOMPLETE_REQUEST_CODE_DESTINATION);
                break;
        }
    }

    private void zoomCamera(){

        if(sourceMarker != null && destinationMarker != null && mMap != null){
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            //the include method will calculate the min and max bound.
            builder.include(sourceMarker.getPosition());
            builder.include(destinationMarker.getPosition());

            LatLngBounds bounds = builder.build();

            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.20); // offset from edges of the map 10% of screen

            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            mMap.moveCamera(cu);
            mMap.animateCamera(cu);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case PLACE_AUTOCOMPLETE_REQUEST_CODE_SOURCE:
                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(this, data);
                    sourceET.setText(place.getAddress());
                    if(sourceMarker != null){
                        sourceMarker.remove();
                    }
                    clearPolyline();
                    bottomSheet.setVisibility(View.GONE);
                    sourceMarker = setMarker(place.getLatLng().latitude, place.getLatLng().longitude, "Source", BitmapDescriptorFactory.HUE_RED);

                    if(destinationMarker != null){
                        List<LatLng> list = new ArrayList<>();
                        list.add(new LatLng(sourceMarker.getPosition().latitude, sourceMarker.getPosition().longitude));
                        list.add(new LatLng(destinationMarker.getPosition().latitude, destinationMarker.getPosition().longitude));
                        drawRouteOnMap(new LatLng(sourceMarker.getPosition().latitude, sourceMarker.getPosition().longitude),
                                new LatLng(destinationMarker.getPosition().latitude, destinationMarker.getPosition().longitude));
                    }

                }
                break;
            case PLACE_AUTOCOMPLETE_REQUEST_CODE_DESTINATION:

                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(this, data);
                    destinationET.setText(place.getAddress());
                    if(destinationMarker != null){
                        destinationMarker.remove();
                    }
                    clearPolyline();
                    bottomSheet.setVisibility(View.GONE);
                    destinationMarker = setMarker(place.getLatLng().latitude, place.getLatLng().longitude, "Destination", BitmapDescriptorFactory.HUE_RED);
                    if(sourceMarker != null){
                        List<LatLng> list = new ArrayList<>();
                        list.add(new LatLng(sourceMarker.getPosition().latitude, sourceMarker.getPosition().longitude));
                        list.add(new LatLng(destinationMarker.getPosition().latitude, destinationMarker.getPosition().longitude));
                        drawRouteOnMap(new LatLng(sourceMarker.getPosition().latitude, sourceMarker.getPosition().longitude),
                                new LatLng(destinationMarker.getPosition().latitude, destinationMarker.getPosition().longitude));
                    }

                }
                break;

        }
    }

    private Observer<List<List<HashMap<String,String>>>> getParsedDataObserver(){
        return new Observer<List<List<HashMap<String, String>>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onNext(@NonNull List<List<HashMap<String, String>>> lists) {
                onFinishedParsing(lists);
                zoomCamera();
            }

            @Override
            public void onError(@NonNull Throwable e) {

            }

            @Override
            public void onComplete() {
                if(progressDialog.isShowing()){
                    progressDialog.dismiss();
                }
            }
        };
    }

    private Observable<List<List<HashMap<String,String>>>> getParsedDataObservable(final GoogleDirectionsResponse response){
        return new Observable<List<List<HashMap<String, String>>>>() {
            @Override
            protected void subscribeActual(Observer<? super List<List<HashMap<String, String>>>> observer) {
                List<List<HashMap<String, String>>> routes = null;

                try {
                    DataParser parser = new DataParser();
                    routes = parser.parse(response);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                observer.onNext(routes);
                observer.onComplete();
            }
        };
    }


    private void drawRouteOnMap(LatLng source, LatLng destination){
        progressDialog.setMessage("Fetching directions...");
        progressDialog.show();
        fetchDirection(String.valueOf(source.latitude), String.valueOf(source.longitude), String.valueOf(destination.latitude), String.valueOf(destination.longitude));
    }


    @Override
    public void onPolylineClick(Polyline polyline) {

        for(int i = 0; i < polylines.length; i++){
            if(polyline.equals(polylines[i])){
                writeDirections(i);
                changeColorPolyLine(i);
            }
        }
    }


    private void writeDirections(int index){
        List<String> directionList = DataParser.getOrderedDirections(index);
        bottomSheet.setVisibility(View.VISIBLE);
        directionsTV.setText("");
        for(int i = 0; i < directionList.size(); i++){
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                directionsTV.append("\n" + (i + 1) + ". ");
                directionsTV.append(Html.fromHtml(directionList.get(i),Html.FROM_HTML_MODE_LEGACY));
            } else {
                directionsTV.append(Html.fromHtml(directionList.get(i)));
            }
        }
    }

    private void changeColorPolyLine(int index){

        for(int i = 0; i < polylines.length; i++){
            if(i != index){
                polylines[i].setColor(Color.RED);
                polylines[i].setZIndex(0);
            }
        }

        for(int i = 0; i < polylines.length; i++){
            if(i == index){
                polylines[i].setColor(Color.BLUE);
                polylines[i].setZIndex(1.0f);
            }
        }
    }


    private void onFinishedParsing(List<List<HashMap<String, String>>> result) {
        ArrayList<LatLng> points;
        PolylineOptions lineOptions = null;

        polylines = new Polyline[result.size()];
        for (int i = 0; i < result.size(); i++) {
            points = new ArrayList<>();
            lineOptions = new PolylineOptions();

            List<HashMap<String, String>> path = result.get(i);

            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);

                points.add(position);
            }

            lineOptions.addAll(points);
            lineOptions.width(10);
            lineOptions.color(Color.RED);

            if(lineOptions != null && mMap != null) {
                polylines[i] = mMap.addPolyline(lineOptions);
                polylines[i].setClickable(true);
            }
            else {
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }
}
