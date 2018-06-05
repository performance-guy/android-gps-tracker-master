package com.ods.salman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ods.salman.fragments.ChangePushTimeFragment;
import com.ods.salman.services.PushLocationService;

import java.lang.reflect.Field;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_CONTACTS;

public class UbidotsActivity extends Activity implements
        ChangePushTimeFragment.DialogListener {
    // Preferences
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    /**
     * Id to identity Location permission request.
     */
    private static final int REQUEST_READ_LOCATION = 0;

    // App info
    private int mTimeToPush = 1;
    private boolean mAlreadyRunning;

    // Activity stuff
    private Button mPushTimeButton;

    private Switch mSwitch;
    // Check connection
    private ConnectionStatusReceiver mReceiver = new ConnectionStatusReceiver();
    private IntentFilter iFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

    // Maps variables
    private GoogleMap mGoogleMap;
    private LatLng mUserLocation;
    private Marker mUserMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ubidots);
        if (getActionBar() != null) {
            // Modify the ActionBar
            getActionBar().setDisplayShowHomeEnabled(false);
            getActionBar().setHomeButtonEnabled(false);
            getActionBar().setDisplayUseLogoEnabled(false);
            getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            getActionBar().setCustomView(R.layout.action_bar);

            // Set the options overflow menu always on top
            try {
                ViewConfiguration config = ViewConfiguration.get(this);
                Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
                if (menuKeyField != null) {
                    menuKeyField.setAccessible(true);
                    menuKeyField.setBoolean(config, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Instantiate layout widgets
        mSwitch = (Switch) findViewById(R.id.toggleActivation);
        mPushTimeButton = (Button) findViewById(R.id.push_times_button);

        // Instantiate shared preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();

        // Get preferences variables
        boolean firstTime =
                mSharedPreferences.getBoolean(Constants.FIRST_TIME, true);
        mAlreadyRunning =
                mSharedPreferences.getBoolean(Constants.SERVICE_RUNNING, false);
        mTimeToPush = mSharedPreferences.getInt(Constants.PUSH_TIME, 1);

        // Set the text at the left of the Switch
        ((TextView) findViewById(R.id.toggleText)).setText((mAlreadyRunning) ?
                getString(R.string.enabled_text) : getString(R.string.disabled_text));
        // Put the switch at its position
        mSwitch.setChecked(mAlreadyRunning);

        // If it's the first time the user access to the application we should put the preference
        // about it into false, so we can continue entering this activity immediately
        if (firstTime) {
            mEditor.putBoolean(Constants.FIRST_TIME, false);
            mEditor.apply();
        }
        MapActivity();


        // Update the text inside the button with the time to push
        updatePushButton();

        // Open the time dialog and
        mPushTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangePushTimeFragment fragment = new ChangePushTimeFragment();
                fragment.show(getFragmentManager(), "UBIDOTS_DIALOG_PUSH");
            }
        });

        // Set the listener when clicked the switch button
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!mAlreadyRunning) {
                    startRepeatingService();
                    mEditor.putBoolean(Constants.SERVICE_RUNNING, true);
                    ((TextView) findViewById(R.id.toggleText)).setText(getString(R.string.enabled_text));
                    createNotification();
                } else {
                    mEditor.putBoolean(Constants.SERVICE_RUNNING, false);
                    ((TextView) findViewById(R.id.toggleText)).setText(getString(R.string.disabled_text));
                    deleteNotification();
                }
                mAlreadyRunning = !mAlreadyRunning;
                mEditor.apply();
            }
        });
    }

    private void MapActivity() {
        // Check if Google Maps is installed
        if (isGoogleMapsInstalled()) {
            // Instantiate the fragment containing the map in the layout
            mGoogleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            // Get the location given by the system
            LocationManager location = (LocationManager) getSystemService(LOCATION_SERVICE);

            // Create a location that updates when the location has changed
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    mUserLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mGoogleMap.clear();
                    mUserMarker = mGoogleMap.addMarker(new MarkerOptions()
                            .position(mUserLocation)
                            .title(getString(R.string.location)));
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mUserLocation, 17));
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

            // Set the listener to the location manager
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            location.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }

    // Start the service
    public void startRepeatingService() {
        startService(new Intent(this, PushLocationService.class));
    }

    // Update the button to show the correct message
    public void updatePushButton() {
        String buttonMsg;
        if (mTimeToPush < 60) {
            buttonMsg = getResources()
                    .getQuantityString(R.plurals.pushes_seconds, mTimeToPush, mTimeToPush);
        } else {
            buttonMsg = getResources()
                    .getQuantityString(R.plurals.pushes_minutes, mTimeToPush / 60, mTimeToPush / 60);
        }
        mPushTimeButton.setText(buttonMsg);
    }

    // Check if Google Maps is installed
    public boolean isGoogleMapsInstalled()
    {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0);
            return true;
        } catch(PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // Check if Google Play is available
    public void checkGooglePlayAvailability() {
        int requestCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (requestCode != ConnectionResult.SUCCESS) {
            GooglePlayServicesUtil.getErrorDialog(requestCode, this, 1337).show();
        }
    }

    // Create the notification to notify the user that the service is running
    public void createNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        String notificationTitle = getString(R.string.notification_title);

        Intent intent = new Intent(this, UbidotsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // We should use Compat, because we are using API 14+
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                .setContentTitle(notificationTitle)
                .setSmallIcon(R.drawable.ic_stat_notify_logo)
                .setContentIntent(pendingIntent);

        // Build the notification
        Notification notificationCompat = notification.build();

        // Create the manager
        NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
        notificationCompat.flags |= Notification.FLAG_ONGOING_EVENT;

        // Push the notification
        notificationManager.notify(Constants.NOTIFICATION_ID, notificationCompat);
    }

    // Delete the notification
    public void deleteNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
        notificationManager.cancel(Constants.NOTIFICATION_ID);
    }

    // Method from the dialog to handle the click
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, int which) {
        if (which == 0) {
            mTimeToPush = 1;
        } else if (which == 1) {
            mTimeToPush = 10;
        } else if (which == 2) {
            mTimeToPush = 30;
        } else if (which == 3) {
            mTimeToPush = 60;
        }

        // Add the time to push in the preferences
        mEditor.putInt(Constants.PUSH_TIME, mTimeToPush);
        mEditor.apply();

        // Update the text from the button
        updatePushButton();
    }

    @Override
    protected void onPause() {
        this.unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        this.registerReceiver(mReceiver, iFilter);
        checkGooglePlayAvailability();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ubidots, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_change_token) {
            mEditor.putBoolean(Constants.SERVICE_RUNNING, false);
            mEditor.putBoolean(Constants.FIRST_TIME, true);
            mEditor.putString(Constants.VARIABLE_ID, null);
            mEditor.putString(Constants.TOKEN, null);
            mEditor.putString(Constants.DATASOURCE_VARIABLE, null);
            mEditor.putInt(Constants.PUSH_TIME, 1);
            mEditor.apply();

            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
        } else if (id == R.id.action_help) {
//            String urlHelp = Constants.BROWSER_CONFIG.HELP_URL;
//            Intent i = new Intent(Intent.ACTION_VIEW);
//            i.setData(Uri.parse(urlHelp));
//            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    public class ConnectionStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int connectionStatus = NetworkUtil.getConnectionStatus(context);
            if (connectionStatus == NetworkUtil.TYPE_MOBILE ||
                    connectionStatus == NetworkUtil.TYPE_WIFI) {
                mPushTimeButton.setEnabled(true);
                mSwitch.setEnabled(true);
            } else {
                if (mSharedPreferences.getBoolean(Constants.SERVICE_RUNNING, false)) {
                    deleteNotification();
                }
                mPushTimeButton.setEnabled(false);
                mSwitch.setChecked(false);
                mSwitch.setEnabled(false);
                mEditor.putBoolean(Constants.SERVICE_RUNNING, false);
                mEditor.apply();
            }
        }
    }
    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            // TODO: alert the user with a Snackbar/AlertDialog giving them the permission rationale
            // To use the Snackbar from the design support library, ensure that the activity extends
            // AppCompatActivity and uses the Theme.AppCompat theme.
        } else {
            requestPermissions(new String[]{ACCESS_COARSE_LOCATION,ACCESS_FINE_LOCATION}, REQUEST_READ_LOCATION);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                MapActivity();
            }
        }
    }

}
