package com.example.saurmn.routetracker4u;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class RouteTracker4UActivity extends FragmentActivity implements GoogleMap.OnMyLocationChangeListener{

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private UiSettings mapSettings;
    private Location currentLocation;
    private Location prevLocation;
    private LatLng currentLatLng;
    private LatLng prevLatLng;
    private LatLng startLatLng;
    private LatLng endLatLng;
    private long distanceTraveled; // total distance travelled by the user
    private LocationManager locationManager;
    private long startTime;
    private Boolean tracking;
    private PolylineOptions polylineOptions;

    private static final double MILLISECONDS_PER_HOUR = 1000 * 60 * 60;
    private static final double MILES_PER_KILOMETER = 0.621371192;
    private static final int MAP_ZOOM = 18; // Google Maps supports 1-21
    private static final int MAP_BEARING = 180;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_tracker_4_u);
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(status != ConnectionResult.SUCCESS){
            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
        }else{
            setUpMapIfNeeded();

            ToggleButton trackingToggleButton = (ToggleButton) findViewById(R.id.trackingToggleButton);
            distanceTraveled = 0;//initializing
            tracking = false;

            trackingToggleButton.setOnCheckedChangeListener(trackingToggleButtonListener);

        }

    }

    OnCheckedChangeListener trackingToggleButtonListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            if(!isChecked){
                //provide details
                tracking = false;
                startLatLng = null;
                endLatLng = currentLatLng;

                mMap.addMarker(new MarkerOptions().position(endLatLng).title("Stop Position"));

                // compute the total time we were tracking
                long milliseconds = System.currentTimeMillis() - startTime;
                double totalHours = milliseconds / MILLISECONDS_PER_HOUR;

                // create a dialog displaying the results
                AlertDialog.Builder dialogBuilder =
                        new AlertDialog.Builder(RouteTracker4UActivity.this);
                dialogBuilder.setTitle(R.string.results);

                double distanceKM = distanceTraveled / 1000.0;
                double speedKM = distanceKM / totalHours;
                double distanceMI = distanceKM * MILES_PER_KILOMETER;
                double speedMI = distanceMI / totalHours;

                // display distanceTraveled traveled and average speed
                dialogBuilder.setMessage(String.format(
                        getResources().getString(R.string.results_format),
                        distanceKM, distanceMI, speedKM, speedMI));

                dialogBuilder.setPositiveButton(
                        R.string.button_ok, null);
                dialogBuilder.show(); // display the dialog

            }else{

                //clear old
                mMap.clear();
                distanceTraveled = 0;

                //start tracking
                currentLatLng = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                startLatLng = currentLatLng;//store start location
                tracking = true;
                startTime = System.currentTimeMillis(); // get current time

                mMap.addMarker(new MarkerOptions().position(startLatLng).title("Start Position"));

                endLatLng = null; // starting a new route
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }


    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationChangeListener(this);

            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

        getMyCurrentLocation();

        if(currentLocation != null) {

            currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
           //mMap.addMarker(new MarkerOptions().position(currentLatLng).title(currentLatLng.toString()));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(currentLatLng)
                    .zoom(MAP_ZOOM)
                    .bearing(MAP_BEARING)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }else{
            //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(0, 0))
                    .zoom(MAP_ZOOM)
                    .bearing(MAP_BEARING)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }


    // get my current location
    public void getMyCurrentLocation(){

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);


        //currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            // Finds a provider that matches the criteria
            String provider = locationManager.getBestProvider(criteria, true);
            // Use the provider to get the last known location

        locationManager.requestLocationUpdates(
                provider, 20000,
                10, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {

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
                });
           /* locationManager.requestLocationUpdates(provider, 20000, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

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
            });*/

            currentLocation = locationManager.getLastKnownLocation(provider);

    }


    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.route_tracker_4_u_menu,menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.mapItem://display map image
                if (mMap != null) {
                    Toast.makeText(this, "Map view selected", Toast.LENGTH_SHORT).show();
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                return true;
            case R.id.satelliteItem://display satellite Image
                if (mMap != null) {
                    Toast.makeText(this,"Satellite view selected", Toast.LENGTH_SHORT).show();
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    public void onMyLocationChange(Location location) {
        // Creating a LatLng object for the current location
        prevLocation = currentLocation;
        currentLocation = location;


        if(tracking) {

            distanceTraveled = distanceTraveled +(long) prevLocation.distanceTo(currentLocation);

            // draw line between location
            polylineOptions = new PolylineOptions().width(10).color(Color.BLUE).geodesic(true);

            prevLatLng = new LatLng(prevLocation.getLatitude(),prevLocation.getLongitude());
            currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            polylineOptions.add(prevLatLng);
            polylineOptions.add(currentLatLng);

            mMap.addPolyline(polylineOptions);

            //mMap.addMarker(new MarkerOptions().position(currentLatLng).title(currentLatLng.toString()));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(currentLatLng)
                    .zoom(MAP_ZOOM)
                    .bearing(MAP_BEARING)
                    .build();

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

    }

}
