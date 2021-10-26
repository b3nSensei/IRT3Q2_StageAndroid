package com.example.exemple;

import android.graphics.Bitmap;
import android.location.Location;

import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.model.BitmapDescriptor;
import com.google.android.libraries.maps.model.BitmapDescriptorFactory;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.Marker;
import com.google.android.libraries.maps.model.MarkerOptions;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

//Spawn handling
public class Spawn {
    public static void spawn(List<Marker> spawn, Location location, Marker mSpawn, GoogleMap mMap, Bitmap b){
        Calendar cal = Calendar.getInstance();
        int minute = cal.get(Calendar.MINUTE);
        int sTime = minute%5;
        //12 hour format
        //int hour = cal.get(Calendar.HOUR);
        //24 hour format
        //int hourofday = cal.get(Calendar.HOUR_OF_DAY);

        //Spawn every 5 minutes
        if (sTime != 0){
            if (spawn.isEmpty()){
                Random rn = new Random();
                int rnd = rn.nextInt(3 - 1 + 1) + 1;
                int height = 75;
                int width = 75;
                Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
                BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);
                LatLng newLatLng = null;
                int rndHp = 0;

                //Generate spawn
                for (int i = 0; i < rnd; i++){
                    rndHp = rn.nextInt(10 - 1 + 1) + 1;
                    double mLat = rn.nextInt(10 + 10) - 10;
                    double mLong = rn.nextInt(10 + 10) - 10;
                    // number of km per degree = ~111km
                    // 1km in degree = 1 / 111.32km = 0.0089
                    // 1m in degree = 0.0089 / 1000 = 0.0000089
                    double coefLat = mLat * 0.0000089;
                    double coefLong = mLong * 0.0000089;
                    double new_lat = location.getLatitude() + coefLat;
                    // pi / 180 = 0.018
                    double new_long = location.getLongitude() + coefLong / Math.cos(new_lat * 0.018);
                    newLatLng = new LatLng(new_lat, new_long);
                    final MarkerOptions pSpawn = new MarkerOptions()
                            .position(newLatLng)
                            .title("spawn")
                            .snippet(String.valueOf(rndHp))
                            .icon(smallMarkerIcon);
                    mSpawn = mMap.addMarker(pSpawn);
                    spawn.add(mSpawn);
                }
            }
        }
        else {
            for (int i = 0; i < spawn.size(); i++){
                mSpawn = spawn.get(i);
                mSpawn.remove();
            }
            //Vide les spawn
            spawn.clear();
        }
    }
}
