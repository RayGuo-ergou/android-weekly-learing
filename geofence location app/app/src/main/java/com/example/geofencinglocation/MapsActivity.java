package com.example.geofencinglocation;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.example.geofencinglocation.Interface.IOnLoandLocationListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryEventListener, IOnLoandLocationListener {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentUser;
    private DatabaseReference myLocationRef;
    private GeoFire geoFire;
    private List<LatLng> dangerousArea;
    private IOnLoandLocationListener listener;
    private DatabaseReference myCity;
    private Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        buildLocationRequest();
                        buildLocationCallback();
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);



                        initArea();
                        settingGeoFire();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this, "permission required ", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();


    }

    private void initArea() {

        myCity =FirebaseDatabase.getInstance()
                .getReference("DangerousAre")
                .child("MyCity");
        listener = this;
        //load form firebase

                myCity.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<MyLatLng> latLngList = new ArrayList<>();
                        for(DataSnapshot locationSnapshot: snapshot.getChildren()){
                            MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                            latLngList.add(latLng);
                        }
                        listener.onLoadLocationSuccess(latLngList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                myCity.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //update dangerous area list
                        List<MyLatLng> latLngList = new ArrayList<>();
                        for(DataSnapshot locationSnapshot: snapshot.getChildren()){
                            MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                            latLngList.add(latLng);
                        }

                        listener.onLoadLocationSuccess(latLngList);


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


//        dangerousArea = new ArrayList<>();
//        dangerousArea.add(new LatLng(37.3108,-122.0332));
//        dangerousArea.add(new LatLng(37.3108,-122.1332));
//        dangerousArea.add(new LatLng(37.3108,-122.2332));
/*
        FirebaseDatabase.getInstance()
                .getReference("DangerousAre")
                .child("MyCity")
                .setValue(dangerousArea)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MapsActivity.this, "Updated!", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });*/



    }




    private void settingGeoFire() {
        myLocationRef = FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire = new GeoFire(myLocationRef);

    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (mMap != null) {
                    lastLocation = locationResult.getLastLocation();
                    geoFire.setLocation("You", new GeoLocation(lastLocation.getLatitude(),
                            lastLocation.getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (currentUser != null) {
                                currentUser.remove();
                            }
                            currentUser = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(lastLocation.getLatitude(),
                                            lastLocation.getLongitude()))
                                    .title("You"));
                            //after add maker, move camera
                            mMap.animateCamera(CameraUpdateFactory
                                    .newLatLngZoom(currentUser.getPosition(), 12.0f));
                        }
                    });
                }

            }


        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (fusedLocationProviderClient != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
        if (geoFire != null)
            geoFire = new GeoFire(myLocationRef);

        //add circle for dangerous
        for (LatLng latLng: dangerousArea){
            mMap.addCircle(new CircleOptions().center(latLng)
            .radius(500)
            .strokeColor(Color.BLUE)
            .fillColor(0x220000ff)
            .strokeWidth(5.0f));

            //create GeoQuery when user in dangerous location
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude,latLng.longitude), 0.5f);
            geoQuery.addGeoQueryEventListener(MapsActivity.this);
        }
    }

    @Override
    protected void onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("EDMTDev", String.format("%s entered the dangerous are", key));

    }

    @Override
    public void onKeyExited(String key) {
        sendNotification("EDMTDev", String.format("%s leave the dangerous are", key));
    }



    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("EDMTDev", String.format("%s move within the dangerous are", key));
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String title, String content) {
        Toast.makeText(this,content,Toast.LENGTH_SHORT).show();

        String NOTIFICATION_CHANNEL_ID = "edmt_multiple_location";
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,"My Notifacation",
                    NotificationManager.IMPORTANCE_DEFAULT);

            //CONFIG
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[] {0,1000,500,1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);

        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        Notification notification = builder.build();
        notificationManager.notify(new Random().nextInt(),notification);
    }

    @Override
    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        dangerousArea = new ArrayList<>();
        for(MyLatLng myLatLng: latLngs){
            LatLng convert = new LatLng(myLatLng.getLatitude(), myLatLng.getLongitude());
            dangerousArea.add(convert);
        }
        //after dangerous area has data, we call the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
        //clear map and add again
        if (mMap != null){
            mMap.clear();
            //add maker again
            geoFire.setLocation("You", new GeoLocation(lastLocation.getLatitude(),
                    lastLocation.getLongitude()), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    if (currentUser != null) {
                        currentUser.remove();
                    }
                    currentUser = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(lastLocation.getLatitude(),
                                    lastLocation.getLongitude()))
                            .title("You"));
                    //after add maker, move camera
                    mMap.animateCamera(CameraUpdateFactory
                            .newLatLngZoom(currentUser.getPosition(), 12.0f));
                }
            });
            if (geoFire != null)
                geoFire = new GeoFire(myLocationRef);

            //add circle of dangerous area
            for (LatLng latLng: dangerousArea){
                mMap.addCircle(new CircleOptions().center(latLng)
                        .radius(500)
                        .strokeColor(Color.BLUE)
                        .fillColor(0x220000ff)
                        .strokeWidth(5.0f));

                //create GeoQuery when user in dangerous location
                GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude,latLng.longitude), 0.5f);
                geoQuery.addGeoQueryEventListener(MapsActivity.this);
            }

        }
    }

    @Override
    public void onLoadLocationFailed(String message) {
        Toast.makeText(this, ""+message, Toast.LENGTH_SHORT).show();
    }
}