package dv606.sb223df.moveit;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateUtils;
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
import java.util.List;

/**
 * Created by Simon on 21/05/2016.
 * Activity to display the history of a particular run, showing the map and statistics
 */
public class SingleRunHistoryActivity extends FragmentActivity implements OnMapReadyCallback {

    private Run run;
    private GoogleMap mMap;
    private TextView dateTV;
    private TextView distanceTV;
    private TextView averageSpeedTV;
    private TextView timeTV;
    private String SPEED_UNIT, DISTANCE_UNIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_run_history);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Manage preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SPEED_UNIT = sp.getString("speed_preference", getString(R.string.kilometer_hour));
        DISTANCE_UNIT = sp.getString("distance_preference", getString(R.string.meter));

        // We get the run id from the History activity
        long id = getIntent().getLongExtra("run_id", -1);
        if (id == -1) {
            System.out.println("ERROR, WRONG ID");
        } else {
            RunDataSource datasource = new RunDataSource(this);
            datasource.open();
            run = datasource.getRun(id);
            datasource.close();
        }

        dateTV = (TextView) findViewById(R.id.date_tv);
        distanceTV = (TextView) findViewById(R.id.distance_tv);
        averageSpeedTV = (TextView) findViewById(R.id.average_speed_tv);
        timeTV = (TextView) findViewById(R.id.time_tv);
        updateUI();
    }

    private void updateUI() {

        // Date & Time
        dateTV.setText(getString(R.string.run_tv) + " " + run.getDate());
        timeTV.setText(String.valueOf(DateUtils.formatElapsedTime(run.getTime())));

        // Distance
        double multiplicator = 0.0;
        DecimalFormat df = null;
        if (DISTANCE_UNIT.equals(getString(R.string.meter))) {
            multiplicator = 1;
            df = new DecimalFormat("0");
        } else if (DISTANCE_UNIT.equals(getString(R.string.kilometer))) {
            multiplicator = 0.001;
            df = new DecimalFormat("##.###");
        }
        distanceTV.setText(df.format(run.getDistance() * multiplicator) + " " + DISTANCE_UNIT);

        // Average speed
        if (SPEED_UNIT.equals(getString(R.string.kilometer_hour))) {
            multiplicator = 3.6;
        } else if (SPEED_UNIT.equals(getString(R.string.meter_second))) {
            multiplicator = 1;
        }
        double averageSpeed = (run.getDistance()/run.getTime())*multiplicator;
        averageSpeedTV.setText(String.valueOf(new DecimalFormat("##.##").format(averageSpeed)) + " " + SPEED_UNIT);
    }

    private void updateMap(){
        ArrayList<LatLng> listCoords = getCoords();
        List<Marker> listMarkers = new ArrayList<Marker>();
        Marker marker;
        // Add a marker for start and end of run
        if (listCoords.size() < 2) { return; } // Less than 2 marks on the map
        mMap.addMarker(new MarkerOptions().position(listCoords.get(0)).title("Start"));
        mMap.addMarker(new MarkerOptions().position(listCoords.get(listCoords.size() - 1)).title("End"));

        // Draw polyline
        for (LatLng coord : listCoords) {
            marker = mMap.addMarker(new MarkerOptions().position(coord).draggable(false).visible(false));
            listMarkers.add(marker);
        }
        LatLng polyline_coords[] = new LatLng[listCoords.size()];
        for (int i =0; i < polyline_coords.length; i++) {
            polyline_coords[i] = listCoords.get(i);
        }
        // Adding road
        mMap.addPolyline(new PolylineOptions().width(5).color(Color.RED)
                        .add(polyline_coords)
        );
        // Move the camera with right zoom
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
                mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom -1));
            }
        });
    }

    private LatLngBounds.Builder builder;

    private ArrayList<LatLng> getCoords(){
        ArrayList<LatLng> listToReturn = new ArrayList<>();
        String[] coords = run.getCoordinates().split(RunActivity.SEPARATOR);
        for (int i = 0; i < coords.length ; i++) {
            String[] parts = coords[i].split(",");
            if (parts.length == 2) {
                String lat = parts[0];
                String lng = parts[1];
                LatLng aCoord = new LatLng(Double.valueOf(lat), Double.valueOf(lng));
                listToReturn.add(aCoord);
            }
        }
        if (listToReturn.size() == 0) {
            System.out.println("ERROR HAPPENED WHILE TRYING TO RETRIEVE COORDS");
        }
        return listToReturn;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        updateMap();
    }
}
