package com.ods.salman;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.ods.salman.fragments.MainFragment;

public class MainActivity extends Activity implements MainFragment.MainFragmentButtonsInterface {
    // We want to know if the user has logged in before
    private SharedPreferences mSharedPreferences;
    private boolean mUserFirstTime;
    private FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFragmentManager = getFragmentManager();
        // Get the preference to check if the user has logged in previously
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUserFirstTime = mSharedPreferences.getBoolean(Constants.FIRST_TIME, true);

        if (savedInstanceState == null) {
            if (mUserFirstTime) {
                MainFragment mainFragment = new MainFragment();
                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.fragment_container, mainFragment)
                        .commit();
            } else {
                Intent ubidotsIntent = new Intent(this, UbidotsActivity.class);
                startActivity(ubidotsIntent);
                finish();
            }
        }
    }

    // Method from MainFragment
    @Override
    public void onLoginButtonClick(Fragment fragment) {
        Bundle arguments = new Bundle();
        Intent i =  new Intent(this,LoginActivity.class);
        startActivity(i);

    }

    // Method from MainFragment
    @Override
    public void onSignUpButtonClick(Fragment fragment) {
        Bundle arguments = new Bundle();

    }
}
