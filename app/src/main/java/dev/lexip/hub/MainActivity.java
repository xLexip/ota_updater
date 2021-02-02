package dev.lexip.hub;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.tvLoading)).setText("Beaming data from space...");
        setLoadingScreen(true);

        // Remote Config: Initialize Firebase Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3500)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetch(10);

        loadConfig();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Require Internet Connection: Start Thread to make sure client stays connected to the internet
        new Thread(){
            public void run(){
                while(true) {
                    if (!getNetworkState() && findViewById(R.id.clSplitScreen).getVisibility() == View.GONE) {
                        finish();
                        startActivity(getIntent());
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {}
                    if (getNetworkState() && findViewById(R.id.clSplitScreen).getVisibility() == View.VISIBLE) {
                        finish();
                        startActivity(getIntent());
                    }
                }
            }
        }.start();
    }

    /**
     *  (Re-)Load the Remote Config and refresh the UI
     */
    public void loadConfig(){
        if(!getNetworkState()) {
            setLoadingScreen(true);
            return;
        }
        setLoadingScreen(true);
        ((TextView) findViewById(R.id.tvLoading)).setText("Beaming data from space...");

        // Remote Config: Update the displayed information
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d(TAG, "Config params updated: " + updated);
                        }
                        // Update UI
                        TextView tvRomName = (TextView) findViewById(R.id.tvRomName);
                        String s = mFirebaseRemoteConfig.getString("rom_name");
                        tvRomName.setText(mFirebaseRemoteConfig.getString("rom_name"));

                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setLoadingScreen(false);
                            }
                        }, 1500);
                    }
                });
    }

    public void setLoadingScreen(boolean pState){
        ((TextView)findViewById(R.id.tvLoading)).setText("Whoops!"+System.getProperty("line.separator")+"There's not even a single bit of internet over here."+System.getProperty("line.separator")+"Please check your connection.");
        if(pState){
            findViewById(R.id.clMain).setVisibility(View.GONE);
            findViewById(R.id.clSplitScreen).setVisibility(View.VISIBLE);
            return;
        }
        findViewById(R.id.clSplitScreen).setVisibility(View.GONE);
        findViewById(R.id.clMain).setVisibility(View.VISIBLE);
    }

    public boolean getNetworkState(){
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED)
            return true;
        else
            return false;
    }
}