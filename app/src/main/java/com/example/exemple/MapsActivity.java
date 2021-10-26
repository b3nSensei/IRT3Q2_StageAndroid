package com.example.exemple;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.maps.CameraUpdateFactory;
import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.OnMapReadyCallback;
import com.google.android.libraries.maps.SupportMapFragment;
import com.google.android.libraries.maps.model.BitmapDescriptorFactory;
import com.google.android.libraries.maps.model.CameraPosition;
import com.google.android.libraries.maps.model.Circle;
import com.google.android.libraries.maps.model.CircleOptions;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.Marker;
import com.google.android.libraries.maps.model.MarkerOptions;
import com.google.android.libraries.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowLongClickListener{

    //Constant & variables
    public static final int DEFAULT_LOC_UPDATE = 10, PERMISSIONS_FINE_LOCATION = 99;
    public static final double HIGH_LOC_UPDATE = 1;
    private final long MIN_TIME = 100; //1000 = 1 seconde
    private final double MIN_DIST = 0; // Meter
    public Marker mPlayer, mMarker, mSpawn;
    TextView tv_dHp, tv_vHp;
    Button signOut;
    int pHp = 100, pause = 0, locationCount;

    //Location and map stuff
    private GoogleMap mMap;
    private LocationListener locationListener;
    private LocationManager locationManager;

    // List of saved location
    List<Marker> allMarker = new ArrayList<Marker>();
    List<Marker> spawn = new ArrayList<Marker>();
    List<JSONObject> allJson = new ArrayList<JSONObject>();
    String poiTitle, poiSnippet, poiTag;
    Double poiLat, poiLong;
    int poiType;

    //Config file for all settings related to FusedLocation
    LocationRequest locationRequest;

    //Google API location service
    FusedLocationProviderClient fusedLocationProviderClient;

    //Creating the location Callback
    LocationCallback locationCallBack;

    //User variable
    GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        //Ui element
        tv_dHp = findViewById(R.id.tv_dHp);
        tv_vHp = findViewById(R.id.tv_vHp);
        signOut = findViewById(R.id.button);

        //Set all properties of locationRequest
        locationRequest = new LocationRequest(); //creating object
        locationRequest.setInterval(DEFAULT_LOC_UPDATE); //Frequency for default location check
        locationRequest.setFastestInterval((long) HIGH_LOC_UPDATE); //frequency for high consumption
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //Setting up priority mode

        //Event that is triggered whenever the update interval is met
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
            }
        };

        //Perms
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);

        //Turning on Updates
        updateGPS();
    } // end of oncreate method

    @Override
    public void onMapReady(final GoogleMap googleMap) {

        mMap = googleMap;

        // Set listeners.
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowLongClickListener(this);

        //Set the UI and map type and style
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Call the custom info window adapter for better snippet
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapsActivity.this));

        //Google Sign in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        //User infos
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        //Creation du cercle d'influence du joueur
        final LatLng loc = new LatLng(0, 0);
        CircleOptions circleOptions = new CircleOptions()
                .center(loc)
                .strokeWidth(1)
                .strokeColor(Color.GREEN)
                .fillColor(Color.argb(50, 0, 0, 100))
                .radius(10); // In meters
        //Instancie le cercle sur la map
        final Circle cPlayer = mMap.addCircle(circleOptions);

        //Creation et instantiation de la camera
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .zoom(19) //Higher = closer
                .tilt(30.5f)
                .target(loc)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        //Creation de waypoint manuelle (joueur) conditionnel
        final MarkerOptions player;
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.player_icon);

        //Creation de waypoint manuelle (joueur)
        player = new MarkerOptions()
                .position(loc)
                .snippet("HP: " + String.valueOf(pHp));

        //Getting user info from google acc (thread)
        UserInfos userInfos = new UserInfos();
        try {
            userInfos.getInfo(acct, player, b);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mPlayer = mMap.addMarker(player);

        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getId() == R.id.button) {
                    signOut();
                }
            }
        });

        //Reading JSON: will add all waypoint and area from json to map
        getPoi();

        //Current location
        locationListener = new LocationListener() {

            //If location has changed
            @Override
            public void onLocationChanged(Location location) {
                //Getting new location
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                //Updating position (circle, player and camera)
                mPlayer.setPosition(latLng);
                cPlayer.setCenter(latLng);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                //Spawn Management
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.burger);
                Spawn.spawn(spawn, location, mSpawn, mMap, b);

                Context tContext = getApplicationContext();
                PositionCheck positionCheck = new PositionCheck();

                //Collision check with waypoint
                positionCheck.colisionCheck(allMarker, latLng, pHp, mPlayer, tv_vHp, tContext);

                //CHeck if player is in a defined zone
                positionCheck.zoneCheck(allMarker, pHp, mPlayer, tv_vHp, tContext);

            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, (float) MIN_DIST, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    } //End of MapReady

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        finish();
                    }
                });
    }

    private void getPoi() {
        String json;
        try {
            //Fetching Json to parse
            InputStream is = getAssets().open("POI.JSON");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            //Setting buffer and array
            json = new String(buffer, "UTF-8");
            JSONArray jsonArray = new JSONArray(json);

            //Looping to get all object from file
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                allJson.add(obj);
                add_wp(obj);
            }
        } catch (Exception e) {
            System.out.print("Error loading Json");
        }
        drawPoly();
    }//End of get json

    private void add_wp(JSONObject obj) {
        //Ajout Waypoint en loop
        //Title, lat, long, snippet and tag are mandatory
        try {
            poiTitle = obj.getString("title");
            poiLat = obj.getDouble("latitude");
            poiLong = obj.getDouble("longitude");
            poiSnippet = obj.getString("snippet");
            poiType = obj.getInt("type");
            poiTag = obj.getString("tag");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Creation des marker
        final LatLng wp = new LatLng(poiLat, poiLong);
        final MarkerOptions poi = new MarkerOptions()
                .position(wp)
                .title(poiTitle)
                .snippet(poiSnippet);

        //Changing marker color and tag according to use.
        //1= informative waypoint, 2= discovery waypoint, 3= secret waypoint
        if (poiType == 1) {
            poi.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            //Adding circle around waypoint
            CircleOptions circleWp = new CircleOptions()
                    .center(wp)
                    .strokeWidth(2)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(0, 0, 0, 0))
                    .radius(10); // In meters
            final Circle cWp = mMap.addCircle(circleWp);
        }
        if (poiType == 2) {
            poi.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            //Adding circle around waypoint
            CircleOptions circleWp = new CircleOptions()
                    .center(wp)
                    .strokeWidth(2)
                    .strokeColor(Color.rgb(0, 204, 0))
                    .fillColor(Color.argb(0, 0, 0, 0))
                    .radius(10); // In meters
            final Circle cWp = mMap.addCircle(circleWp);
        }
        if (poiType == 3) {
            poi.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            poi.visible(false);
            //Adding circle around waypoint
            CircleOptions circleWp = new CircleOptions()
                    .center(wp)
                    .strokeWidth(2)
                    .strokeColor(Color.rgb(220, 220, 0))
                    .fillColor(Color.argb(0, 0, 0, 0))
                    .radius(10); // In meters
            final Circle cWp = mMap.addCircle(circleWp);
        }
        if (poiType == 5) {
            poi.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
            poi.visible(false);
        }

        mMarker = mMap.addMarker(poi);
        mMarker.setTag(poiTag);

        //Saving marker in array
        allMarker.add(mMarker);

        //Keeping count
        locationCount++;
    }//End of add json

    private void drawPoly() {
        for (int i = 0; i < allMarker.size(); i++) {
            Marker point1 = allMarker.get(i);
            String tag1 = (String) point1.getTag();

            if (tag1.equals("secret")){
                System.out.println("bad point");
            }
            else {
                for (int j = 0; j < allMarker.size(); j++) {
                    Marker point2 = allMarker.get(j);
                    String tag2 = (String) point2.getTag();

                    if (tag1.matches(tag2)) {
                        LatLng p1 = point1.getPosition();
                        LatLng p2 = point2.getPosition();

                        //Drawing line between point
                        PolylineOptions poly = new PolylineOptions()
                                .add(p1)
                                .add(p2)
                                .width(8f)
                                .color(Color.RED);
                        mMap.addPolyline(poly);
                    }
                }
            }
        }
    }

    private void updateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);

        //Get permission from user to track GPS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //user has provided permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    startLocUpdate();
                }
            });
        } else { //permission not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //Si build version high enough
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    } //end of updateGPS method

    private void startLocUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //Turning on Updates
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
    }//End of startLocUpdate

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                } else {
                    Toast.makeText(this, "This app requires permission", LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }//En of RequestPermissions

    @Override
    public boolean onMarkerClick(final Marker marker) {
        if(marker.getTitle().equals("spawn")){
            if (pHp == 100) {
                Toast.makeText(getApplicationContext(), "Point de vie au max!", Toast.LENGTH_LONG).show();
            }
            else {
                String addHp = marker.getSnippet();
                pHp++;
                mPlayer.setSnippet("HP: " + String.valueOf(pHp));
                tv_vHp.setText(String.valueOf(pHp));
                marker.remove();
            }
        }
        else {
            marker.showInfoWindow();
        }
        return true;
    }//End of click

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        String cTag = String.valueOf(marker.getTag());

        for (int i = 0; i < allMarker.size(); i++) {
            mMarker = allMarker.get(i);
            String sTitle = mMarker.getTitle();

            //Si le titre d'un marker match le tag du marker clicker
            if (sTitle.matches(cTag)) {
                //turning GPS update of
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
                locationManager.removeUpdates(locationListener);

                //Setting view on marker +showing infos
                LatLng sLatLng = mMarker.getPosition();
                mMap.moveCamera(CameraUpdateFactory.newLatLng(sLatLng));
                mMarker.setVisible(true);
                mMarker.showInfoWindow();

                //Restarting location updates
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startLocUpdate();
                        try {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, (float) MIN_DIST, locationListener);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                    }
                }, 3000);
            }
        }
    }//End of info Window long clic
}//End of MapsActivity
