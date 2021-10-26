package com.example.exemple;

import android.content.Context;
import android.location.Location;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.Marker;

import java.util.List;

public class PositionCheck {

    //Variables
    int locationCount, pause = 0;
    Marker mMarker;
    float distanceInteraction = 20; // meter

    //Check for colision
    public void colisionCheck(List<Marker> allMarker, LatLng latLng, int pHp, Marker mPlayer, TextView tv_vHp, Context context){

        //Loop all marker to check if in range
        for (int i = 0; i < locationCount; i++) {
            mMarker = allMarker.get(i);
            float[] collisionCheck = new float[1];
            Location.distanceBetween(
                    latLng.latitude,
                    latLng.longitude,
                    mMarker.getPosition().latitude,
                    mMarker.getPosition().longitude,
                    collisionCheck);
            if (collisionCheck[0] <= distanceInteraction) {
                mMarker.setVisible(true);
                mMarker.showInfoWindow();

                //Decompte des HP
                pause++;
                if (pHp != 0 && pause >= 10) {
                    pause = 0;
                    pHp--;
                    mPlayer.setSnippet("HP: " + String.valueOf(pHp));
                    tv_vHp.setText(String.valueOf(pHp));
                    if (pHp == 0) {
                        Toast.makeText(context, "Point de vie épuiser! (Colision)", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    //Check if player in given zone
    public void zoneCheck(List<Marker> allMarker, int pHp, Marker mPlayer, TextView tv_vHp, Context context){

        //Loop all marker: first point
        for (int i = 0; i < allMarker.size(); i++) {
            Marker point1 = allMarker.get(i);
            String tag1 = (String) point1.getTag();
            String title1 = (String) point1.getTitle();

            //Loop all marker: second point
            for (int j = 0; j < allMarker.size(); j++) {
                Marker point2 = allMarker.get(j);
                String tag2 = (String) point2.getTag();
                String title2 = (String) point2.getTitle();

                if ( !title1.matches(title2) && tag1.matches(tag2)){

                    //Loop all marker: third point
                    for (int k = 0; k < allMarker.size(); k++) {
                        Marker point3 = allMarker.get(k);
                        String tag3 = (String) point3.getTag();
                        String title3 = (String) point3.getTitle();

                        if ( !title1.matches(title2) && !title2.matches(title3) && tag1.matches(tag3)) {
                            //Player coordinates
                            double xp = mPlayer.getPosition().longitude;
                            double yp = mPlayer.getPosition().latitude;
                            //Point 1 Coordinate
                            double x1 = point1.getPosition().longitude;
                            double y1 = point1.getPosition().latitude;
                            //Point 1 Coordinate
                            double x2 = point2.getPosition().longitude;
                            double y2 = point2.getPosition().latitude;
                            //Point 1 Coordinate
                            double x3 = point3.getPosition().longitude;
                            double y3 = point3.getPosition().latitude;

                            // Check whether the point P(10, 15) lies inside the triangle formed by A(0, 0), B(20, 0) and C(10, 30)
                            if (isInside(x1, y1, x2, y2, x3, y3, xp, yp)) {
                                //Is inside
                                pause++;
                                if (pHp != 0 && pause >= 10) {
                                    pause = 0;
                                    pHp--;
                                    mPlayer.setSnippet("HP: " + String.valueOf(pHp));
                                    tv_vHp.setText(String.valueOf(pHp));
                                    if (pHp == 0) {
                                        Toast.makeText(context, "Point de vie épuiser! (zone)", Toast.LENGTH_LONG).show();
                                        pHp = 100;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isInside(double x1, double y1, double x2, double y2, double x3, double y3, double xp, double yp) {
        //Calculate area of triangle ABC
        double A = area (x1, y1, x2, y2, x3, y3);
        //Calculate area of triangle PBC
        double A1 = area (xp, yp, x2, y2, x3, y3);
        //Calculate area of triangle PAC
        double A2 = area (x1, y1, xp, yp, x3, y3);
        //Calculate area of triangle PAB
        double A3 = area (x1, y1, x2, y2, xp, yp);

        //Check if sum of A1, A2 and A3 is same as A
        return (A == A1 + A2 + A3);
    }

    // A function to calculate area of triangle formed by (x1, y1) (x2, y2) and (x3, y3)
    static double area(double x1, double y1, double x2, double y2, double x3, double y3) {
        return Math.abs((x1*(y2-y3) + x2*(y3-y1)+x3*(y1-y2))/2.0);
    }
}
