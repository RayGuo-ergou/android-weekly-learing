package com.example.geofencinglocation.Interface;

import com.example.geofencinglocation.MyLatLng;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;


public interface IOnLoandLocationListener {
    void onLoadLocationSuccess(List<MyLatLng> latLngs);
    void onLoadLocationFailed(String message);
}
