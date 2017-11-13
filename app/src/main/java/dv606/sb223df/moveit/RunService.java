package dv606.sb223df.moveit;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Simon on 05/05/2016.
 * Background service listenign to location update and that send this location to the run activity
 * (create the notification also)
 * /!\ NOTE : During all the service, Android Studio requires the check the permission (like in line 65) to
 * ACCESS_FINE||COARSE_LOCATION, even if teh permission is already set in the manifest
 */
public class RunService extends Service implements LocationListener {

    private Service main_service;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 2; // 2 seconds

    private LocationManager locMan;
    private String provider = LocationManager.GPS_PROVIDER;


    @Override
    public void onCreate() {
        System.out.println("Run Service created.");
        main_service = this;

        // Manage ongoing notification
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.notification_icon)
                .setWhen(System.currentTimeMillis())
                .setTicker("")
                .setAutoCancel(true);
        builder.setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.click_title))
                .setContentInfo(getString(R.string.click));
        Intent intent = new Intent(this, RunActivity.class);
        PendingIntent notifIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(notifIntent);
        Notification notification = builder.build();
        startForeground(10, notification);

        locMan = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(main_service, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(main_service, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("NO PERMISSION");
        } else {
            locMan.requestLocationUpdates(provider, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            System.out.println("REQUESTING UPDATE");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Start command.");
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        if (ActivityCompat.checkSelfPermission(main_service, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(main_service, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("NO PERMISSION");
        } else {
            locMan.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // When a new location is received
        if (location == null) {
            return;
        } else {
            sendLocationToRunActivity(location);
        }
    }

    private void sendLocationToRunActivity (Location location) {
        Bundle mBundle = new Bundle();
        mBundle.putString("lat", String.valueOf(location.getLatitude()));
        mBundle.putString("lng", String.valueOf(location.getLongitude()));
        Intent local = new Intent();
        local.putExtras(mBundle);
        local.setAction(RunActivity.BROADCAST_ACTION);
        main_service.sendBroadcast(local);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}
