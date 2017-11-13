package dv606.sb223df.moveit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Simon on 04/05/2016.
 * Activity to start a run, show statistics and the road via a Google Map
 */
public class RunActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String BROADCAST_ACTION = "DATA_FROM_SERVICE";
    public static final String SEPARATOR = ";";
    private String SPEED_UNIT, DISTANCE_UNIT;

    private Chronometer chrono;
    private Activity main_activity;
    private BroadcastReceiver updateUIReceiver;
    private boolean running = false;
    private GoogleMap mMap;
    private LatLngBounds.Builder builder;
    private List<LatLng> listCoords = new ArrayList<LatLng>(); // used for polyline
    private List<Marker> listMarkers = new ArrayList<Marker>(); // used for the map zoom
    private TextView distanceTV;
    private TextView speedTV;
    private TextView averageSpeedTV;
    private long lastUpdate = 0;
    private double totalDistance = 0.0;
    private AlertDialog endDialog;
    private String date;
    private int totalTimeSecond;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        main_activity = this;
        System.out.println("Run Activity created.");
        date = DateFormat.format("yyyy-MM-dd hh:mm:ss", Calendar.getInstance().getTime()).toString();;

        // Manage preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SPEED_UNIT = sp.getString("speed_preference", getString(R.string.kilometer_hour));
        DISTANCE_UNIT = sp.getString("distance_preference", getString(R.string.meter));

        // UI
        distanceTV = (TextView) findViewById(R.id.distance_tv);
        distanceTV.setText(DISTANCE_UNIT);
        speedTV = (TextView) findViewById(R.id.speed_tv);
        speedTV.setText(SPEED_UNIT);
        averageSpeedTV = (TextView) findViewById(R.id.average_speed_tv);
        averageSpeedTV.setText(SPEED_UNIT);

        chrono = (Chronometer) findViewById(R.id.chrono);

        Button startStopButton = (Button) findViewById(R.id.button_start_stop);
        startStopButton.setOnClickListener(new StartStopButtonClick());

        // Register Broadcast Receiver
        IntentFilter filter = new IntentFilter();

        filter.addAction(BROADCAST_ACTION);

        updateUIReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // Here we update UI
                // 1) Get the Location
                Bundle mBundle = intent.getExtras();
                float lat = Float.valueOf(mBundle.getString("lat"));
                float lng = Float.valueOf(mBundle.getString("lng"));

                System.out.println("Received : ");
                System.out.println("\tlat: " + lat);
                System.out.println("\tlng: " + lng);

                LatLng coord = new LatLng(lat, lng);

                // 2) Add a marker if it's first location received
                Marker marker;
                if (listCoords.size() == 0) {
                    // We get the first coord, so we add a marker on it
                    marker = mMap.addMarker(new MarkerOptions().position(coord).title("Start"));
                } else {
                    // It's a invisible marker to draw the polyline and use the list of marker for zoom
                    marker = mMap.addMarker(new MarkerOptions().position(coord).draggable(false).visible(false));
                }

                // 3) We add the coord and the marker in the lists
                listCoords.add(coord);
                listMarkers.add(marker);

                // 4) Draw polyline map
                LatLng polyline_coords[] = new LatLng[listCoords.size()];
                for (int i =0; i < polyline_coords.length; i++) {
                    polyline_coords[i] = listCoords.get(i);
                }
                mMap.addPolyline(new PolylineOptions().width(5).color(Color.RED)
                                .add(polyline_coords)
                );

                // 5) Move the camera with right zoom
                builder = new LatLngBounds.Builder();
                for (Marker m : listMarkers) {
                    builder.include(m.getPosition());
                }
                // When the map is loaded, we zoom to fit the markers
                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 15));
                        // After zoom on the road, we dezoom of one to have a better view
                        mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom - 1));
                    }
                });

                // 6) Update statistics
                updateStats();
            }
        };

        registerReceiver(updateUIReceiver, filter);
    }

    private void updateStats(){
        computeAndUpdateSpeed();
        updateDistance();
    }

    /**
     * Method to set the GoogleMap and enable zoom controls
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    private class StartStopButtonClick implements View.OnClickListener {
        public void onClick(View v) {
            if (!running) { // We start the run and the background service
                chrono.setBase(SystemClock.elapsedRealtime());
                Intent serviceIntent = new Intent(main_activity, RunService.class);
                main_activity.startService(serviceIntent);
                chrono.start();
                running = true;
            } else { // We end the run
                chrono.stop();
                stopServiceAndReceiver();
                totalTimeSecond = (int) (SystemClock.elapsedRealtime() - chrono.getBase()) / 1000;
                running = false;
                saveRunningInDB();

                // End Dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(main_activity);
                builder.setMessage(getString(R.string.run_ended) + " " + ((int)totalDistance) + "m in " + DateUtils.formatElapsedTime(totalTimeSecond));

                builder.setNegativeButton(R.string.return_menu, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        main_activity.finish();
                        returnToMainMenu();
                    }
                });

                builder.setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String toShare = "Hey ! I runned " + ((int) totalDistance) + "m in " + DateUtils.formatElapsedTime(totalTimeSecond) + " the " + date;
                        Intent intentSend = new Intent(Intent.ACTION_SEND);
                        String title = "SELECT AN APP TO SHARE";
                        // Create intent to show chooser
                        Intent chooser = Intent.createChooser(intentSend, title);
                        intentSend.setType("text/plain");
                        intentSend.putExtra(Intent.EXTRA_TEXT, toShare);
                        // Verify the intent will resolve to at least one activity
                        if (intentSend.resolveActivity(getPackageManager()) != null) {
                            System.out.println("Start chooser send");
                            startActivity(chooser);
                        }
                        stopServiceAndReceiver();
                        main_activity.finish();
                    }
                });

                // Create the AlertDialog
                endDialog = builder.create();

                endDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // If the user click outside the alert, it will close the activity as if he had choosen the "return to menu"
                        main_activity.finish();
                    }
                });

                endDialog.show();
            }
            manageStartStopButton();
        }
    }

    /**
     * To change the label of the Start/Stop button, depending of the state of running
     */
    private void manageStartStopButton() {
        Button startStopButton = (Button) findViewById(R.id.button_start_stop);
        if (running) {
            startStopButton.setText(R.string.stop_run);
        } else {
            startStopButton.setText(R.string.start_run);
        }
    }

    private void computeAndUpdateSpeed(){

        if (listCoords.size() < 2) return; // We need atleast 2 position to compute the current speed

        int listSize = listCoords.size();

        // Get last coord registred
        LatLng lastCoord = listCoords.get(listSize - 1);
        Location locLast = new Location("");
        locLast.setLatitude(lastCoord.latitude);
        locLast.setLongitude(lastCoord.longitude);

        // Get before last coord registred
        LatLng beforeLastCoor = listCoords.get(listSize-2);
        Location locBeforeLast = new Location("");
        locBeforeLast.setLatitude(beforeLastCoor.latitude);
        locBeforeLast.setLongitude(beforeLastCoor.longitude);

        // Distance in meter
        float distance = locBeforeLast.distanceTo(locLast);
        totalDistance += distance;

        double multiplicator = 0.0;
        if (SPEED_UNIT.equals(getString(R.string.kilometer_hour))) {
            multiplicator = 3.6;
        } else if (SPEED_UNIT.equals(getString(R.string.meter_second))) {
            multiplicator = 1;
        }

        double currentSpeed = calculateCurrentSpeed(distance, multiplicator);
        double averageSpeed = calculateAverageSpeed(multiplicator);

        updateCurrentSpeed(currentSpeed);
        updateAverageSpeed(averageSpeed);
    }

    private double calculateCurrentSpeed(float distance, double multiplicator){
        double timeSinceLastUpdateInSecond = ((SystemClock.elapsedRealtime() - chrono.getBase() - lastUpdate))/1000;
        lastUpdate = (SystemClock.elapsedRealtime() - chrono.getBase());
        double speed = (distance/timeSinceLastUpdateInSecond)*multiplicator;
        return speed;
    }

    private double calculateAverageSpeed(double multiplicator) {
        double totalTimeInSecond = (SystemClock.elapsedRealtime() - chrono.getBase())/1000;
        double averageSpeed = (totalDistance/totalTimeInSecond)*multiplicator;
        return averageSpeed;
    }

    private void updateCurrentSpeed(double speed) {
        speedTV.setText(String.valueOf(new DecimalFormat("0.##").format(speed)) + SPEED_UNIT);
    }

    private void updateAverageSpeed(double speed){
        averageSpeedTV.setText(String.valueOf(new DecimalFormat("0.##").format(speed)) + SPEED_UNIT);
    }

    private void updateDistance() {
        double multiplicator = 0.0;
        DecimalFormat df = null;
        if (DISTANCE_UNIT.equals(getString(R.string.meter))) {
            multiplicator = 1;
            df = new DecimalFormat("0");
        } else if (DISTANCE_UNIT.equals(getString(R.string.kilometer))) {
            multiplicator = 0.001;
            df = new DecimalFormat("##.###");
        }
        distanceTV.setText(String.valueOf(df.format(totalDistance * multiplicator)) + DISTANCE_UNIT);
    }

    private void saveRunningInDB() {
        // Cast is ok here because integer size is enough to store the time
        // (more than 500 hours can be stored in miliseconds in a integer)
        int distanceInMeter = (int) totalDistance;
        // Put all coordinates in a String
        // Format : latitude1,longitude1;latitude2,longitude2;...
        String coordinates = "";
        for (LatLng c : listCoords) {
            if (coordinates.equals("")) {
                // No need to separate first
                coordinates += c.latitude + "," + c.longitude;
            } else {
                coordinates += SEPARATOR + c.latitude + "," + c.longitude;
            }
        }

        RunDataSource datasource = new RunDataSource(this);
        datasource.open();
        Run test = datasource.createRun(coordinates, totalTimeSecond, distanceInMeter, date);
        if(test == null) {
            System.out.println("ERROR DURING CREATION PROCESS, TEST IS NULL");
        }
        datasource.close();
    }

    private void stopServiceAndReceiver(){

        // Service
        Intent serviceIntent = new Intent(main_activity, RunService.class);
        main_activity.stopService(serviceIntent);

        // Receiver
        try {
            main_activity.unregisterReceiver(updateUIReceiver);
        } catch (Exception e) {
            System.out.println("Receiver already unregistred");
        }
    }

    @Override
    public void onBackPressed() {
        if (running) {
            // Ask if sure to quit the run
            AlertDialog.Builder builder = new AlertDialog.Builder(main_activity);
            builder.setMessage(R.string.quit_alert);

            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Nothing
                }
            });

            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    stopServiceAndReceiver();
                    main_activity.finish();
                    returnToMainMenu();
                }
            });

            // Create the AlertDialog
            AlertDialog askDialog = builder.create();
            askDialog.show();
        } else {
            stopServiceAndReceiver();
            finish();
        }
    }

    private void returnToMainMenu() {
        Intent intent = new Intent(main_activity, MainMenu.class);
        startActivity(intent);
    }
}
