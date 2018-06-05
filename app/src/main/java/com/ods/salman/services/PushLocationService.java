package com.ods.salman.services;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.ods.salman.Constants;
import com.ods.salman.Login.AppConfig;
import com.ods.salman.Login.Helper.AppController;
import com.ods.salman.NetworkUtil;
import com.ods.salman.UbidotsActivity;
import com.ods.salman.receivers.PushAlarmReceiver;
import com.ubidots.ApiClient;
import com.ubidots.Variable;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PushLocationService extends Service implements LocationListener {

    // For repeating this service
    private AlarmManager mAlarmManager;
    private PendingIntent mAlarmIntent;
    static String TAG = "Push Location Service";
    // For location updates
    private LocationManager mLocationManager;

    // Read preference data
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get the update frequency
        int updateFreq = mPrefs.getInt(Constants.PUSH_TIME, 1);
        // Get the Network status
        int connectionStatus = NetworkUtil.getConnectionStatus(this);
        // Check if the service is still activated by the user
        boolean isRunning = mPrefs.getBoolean(Constants.SERVICE_RUNNING, false);

        // If the service is activated and we have network connection
        // Create a pending intent
        if (isRunning && (connectionStatus == NetworkUtil.TYPE_MOBILE ||
                connectionStatus == NetworkUtil.TYPE_WIFI)) {
            int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
            long timeToRefresh = SystemClock.elapsedRealtime() + updateFreq * 1000;
            mAlarmManager.setInexactRepeating(alarmType, timeToRefresh, updateFreq * 1000,
                    mAlarmIntent);
        } else {
            deleteNotification();
            mAlarmManager.cancel(mAlarmIntent);
        }

        // If we have network connection
        if ((isRunning) && (connectionStatus == NetworkUtil.TYPE_MOBILE ||
                connectionStatus == NetworkUtil.TYPE_WIFI)) {
            String provider = (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) ?
                    LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;

            mLocationManager.requestLocationUpdates(provider, updateFreq * 1000, 0, this);
        } else {
            mLocationManager.removeUpdates(this);
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        String ALARM_ACTION = PushAlarmReceiver.ACTION_PUSH_LOCATION_ALARM;
        Intent intentToFire = new Intent(ALARM_ACTION);
        mAlarmIntent = PendingIntent.getBroadcast(this, 0, intentToFire, 0);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mEditor = mPrefs.edit();

        initializeLocationUpdates();
    }

    @Override
    public void onDestroy() {
        stopSelf();
        mLocationManager.removeUpdates(this);

        super.onDestroy();
    }

    public void initializeLocationUpdates() {
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }

    public void deleteNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager man = (NotificationManager)
                getApplicationContext().getSystemService(ns);
        man.cancel(Constants.NOTIFICATION_ID);
    }

    @Override
    public void onLocationChanged(Location location) {
        String token = mPrefs.getString(Constants.TOKEN, null);
        new UbidotsAPI(location.getLongitude(), location.getLatitude(),
                location.getAltitude()).execute(token);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    public class UbidotsAPI extends AsyncTask<String, Void, Void> {
        private final String variableID = mPrefs.getString(Constants.VARIABLE_ID, null);
        private double longitude;
        private double latitude;
        private double altitude;

        public UbidotsAPI(double longitude, double latitude, double altitude) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.altitude = altitude;
        }

        @Override
        protected Void doInBackground(String... params) {
//            try {
//                Map<String, Object> context = new HashMap<String, Object>();
//                ApiClient apiClient = new ApiClient().fromToken(params[0]);
//
//                if (variableID != null) {
//                    Variable variable = apiClient.getVariable(variableID);
//                    context.put(Constants.VARIABLE_CONTEXT.LATITUDE, latitude);
//                    context.put(Constants.VARIABLE_CONTEXT.LONGITUDE, longitude);
//
//                    if (!context.get(Constants.VARIABLE_CONTEXT.LATITUDE).equals(0.0) &&
//                            !context.get(Constants.VARIABLE_CONTEXT.LONGITUDE).equals(0.0)) {
//                        variable.saveValue(altitude, context);
//                    }
//                }
//            } catch (Exception e) {
//                Handler h = new Handler(getMainLooper());
//
//                h.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (getApplicationContext() != null) {
//                            Toast.makeText(getApplicationContext(),
//                                    "Error in connection",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                        cancel(true);
//                    }
//                });
////                mEditor.putBoolean(Constants.SERVICE_RUNNING, false);
////                mEditor.apply();
////                stopSelf();
//            }
            String lat = String.valueOf(latitude);
            String lon = String.valueOf(longitude);
            SendLocation(lat,lon);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopSelf();
        }
    }
    private void SendLocation(final String lat, final String lon) {
        // Tag used to cancel the request
        String tag_string_req = "req_location";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_LOCATION, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Location Response: " + response.toString());


                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {

                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Log.i(TAG,errorMsg);
                        Toast.makeText(getApplicationContext(),
                                "Internet Connectivity Failed", Toast.LENGTH_LONG).show();

                    }
                } catch (JSONException e) {
                    // JSON error

                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();

                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {

                Log.e(TAG, "Login Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        "Internet Connectivity Failed", Toast.LENGTH_LONG).show();
                mEditor.putBoolean(Constants.SERVICE_RUNNING, false);
                mEditor.apply();
                stopSelf();
            }

        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("lat", lat);
                params.put("lon", lon);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

}