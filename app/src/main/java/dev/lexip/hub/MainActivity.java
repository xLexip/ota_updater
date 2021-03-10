package dev.lexip.hub;

import dev.lexip.hub.BuildConfig;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private String buildNumber;
    private int clientRomVersion;
    private int latestRomVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.tvLoading)).setText("Beaming data from space...");
        setLoadingScreen(true);

        // Remote Config: Initialize Firebase Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(1000)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetch(1);

        ((TextView) findViewById(R.id.tvAppVersion)).setText("v"+BuildConfig.VERSION_NAME+"  -  github.com/xLexip/ota_updater");
        loadConfig(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Require Internet Connection: Start Thread to make sure client stays connected to the internet
        new Thread(){
            public void run(){
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {}
                while(true) {
                    if (!getNetworkState() && findViewById(R.id.clSplitScreen).getVisibility() == View.GONE) {
                        finish();
                        startActivity(getIntent());
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {}
                    if (getNetworkState() && findViewById(R.id.clSplitScreen).getVisibility() == View.VISIBLE) {
                        finish();
                        startActivity(getIntent());
                    }
                }
            }
        }.start();

        loadConfig(false);
    }

    /**
     *  Remote Config: Fetch, activate and process config
     */
    public void loadConfig(boolean pIninial){
        if(!getNetworkState()) {
            ((TextView)findViewById(R.id.tvLoading)).setText("Whoops!"+System.getProperty("line.separator")+"There's not even a single bit of internet over here."+System.getProperty("line.separator")+"Please check your connection.");
            setLoadingScreen(true);
            return;
        }
        if(pIninial)
            setLoadingScreen(true);

        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d(TAG, "Config params updated: " + updated);
                        }

                        buildNumber = getSystemProperty("org.pixelexperience.version.display");
                        ((TextView) findViewById(R.id.tvLoading)).setText("Beaming data from space...");

                        // Abort if device is incompatible
                        if(!buildNumber.contains(mFirebaseRemoteConfig.getString("verification_string"))) {
                            ((TextView) findViewById(R.id.tvLoading)).setText("Incompatible OS");
                            return;
                        }
                        String test = getSystemProperty("org.pixelexperience.device");
                        if(!getSystemProperty("org.pixelexperience.device").equals("dumpling") && !getSystemProperty("org.pixelexperience.device").equals("cheeseburger")){
                            ((TextView) findViewById(R.id.tvLoading)).setText("Incompatible Device");
                            return;
                        }
                        if(mFirebaseRemoteConfig.getString("app_activated").equals("false")){
                            ((TextView) findViewById(R.id.tvLoading)).setText("SERVICE UNAVAILABLE");
                            return;
                        }
                        if(mFirebaseRemoteConfig.getString("disable_everywhere_irrevocable").equals("true")){
                            try {
                                final Runtime runtime = Runtime.getRuntime();
                                runtime.exec("pm disable dev.lexip.hub");
                                Toast.makeText(MainActivity.this,
                                        "Auto uninstall", Toast.LENGTH_LONG).show();
                                Toast.makeText(MainActivity.this,
                                        "Goodbye, never see you again...", Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // Handle latest ROM Version
                        clientRomVersion = Integer.parseInt((buildNumber.substring(buildNumber.indexOf("-20")+1)).substring(0,8));
                        latestRomVersion = Integer.parseInt(mFirebaseRemoteConfig.getString("latest_rom_version"));
                        ((TextView) findViewById(R.id.tvVersion)).setText("v"+String.valueOf(clientRomVersion));

                        if(clientRomVersion>=latestRomVersion){
                            ((ConstraintLayout) findViewById(R.id.layUpdateSection)).setBackground(getDrawable(R.drawable.layout_green));
                            ((TextView) findViewById(R.id.tvCurrentState)).setText("UPTODATE");
                            ((Button) findViewById(R.id.btnUpdate)).setVisibility(View.GONE);
                        } else {
                            ((ConstraintLayout) findViewById(R.id.layUpdateSection)).setBackground(getDrawable(R.drawable.layout_orange));
                            ((TextView) findViewById(R.id.tvCurrentState)).setText("NEW UPDATE AVAILABLE");
                            ((TextView) findViewById(R.id.tvVersion)).setText("v"+String.valueOf(clientRomVersion)+"  >>  v"+String.valueOf(latestRomVersion));
                            ((Button) findViewById(R.id.btnUpdate)).setVisibility(View.VISIBLE);
                            ((Button) findViewById(R.id.btnUpdate)).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    if(getSystemProperty("org.pixelexperience.device").equals("dumpling"))
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mFirebaseRemoteConfig.getString("dumpling_download"))));
                                    else if(getSystemProperty("org.pixelexperience.device").equals("cheeseburger"))
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mFirebaseRemoteConfig.getString("cheeseburger_download"))));
                                }
                            });

                            // Haptic Feedback
                            new Thread(){
                                public void run(){
                                    ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1);
                                    try {
                                        Thread.sleep(110);
                                    } catch (InterruptedException e) {}
                                    ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1);
                                }
                            }.start();
                        }

                        // Update UI
                        ((TextView) findViewById(R.id.tvAppName)).setText(mFirebaseRemoteConfig.getString("app_name"));
                        ((TextView) findViewById(R.id.tvRomName)).setText(mFirebaseRemoteConfig.getString("rom_name"));
                        ((TextView) findViewById(R.id.tvMaintenanceType)).setText(mFirebaseRemoteConfig.getString("maintenance_type"));
                        ((TextView) findViewById(R.id.tvInfoHeadline)).setText(mFirebaseRemoteConfig.getString("info_headline"));

                        ((Button) findViewById(R.id.btnInfoOne)).setText(mFirebaseRemoteConfig.getString("helpbtn1_text"));
                        ((Button) findViewById(R.id.btnInfoTwo)).setText(mFirebaseRemoteConfig.getString("helpbtn2_text"));
                        ((Button) findViewById(R.id.btnInfoThree)).setText(mFirebaseRemoteConfig.getString("helpbtn3_text"));
                        ((Button) findViewById(R.id.btnInfoFour)).setText(mFirebaseRemoteConfig.getString("helpbtn4_text"));
                        if(mFirebaseRemoteConfig.getString("helpbtn1_text").isEmpty())
                            ((Button) findViewById(R.id.btnInfoOne)).setVisibility(View.GONE);
                        else
                            ((Button) findViewById(R.id.btnInfoOne)).setVisibility(View.VISIBLE);
                        if(mFirebaseRemoteConfig.getString("helpbtn2_text").isEmpty())
                            ((Button) findViewById(R.id.btnInfoTwo)).setVisibility(View.GONE);
                        else
                            ((Button) findViewById(R.id.btnInfoTwo)).setVisibility(View.VISIBLE);
                        if(mFirebaseRemoteConfig.getString("helpbtn3_text").isEmpty())
                            ((Button) findViewById(R.id.btnInfoThree)).setVisibility(View.GONE);
                        else
                            ((Button) findViewById(R.id.btnInfoThree)).setVisibility(View.VISIBLE);
                        if(mFirebaseRemoteConfig.getString("helpbtn4_text").isEmpty())
                            ((Button) findViewById(R.id.btnInfoFour)).setVisibility(View.GONE);
                        else
                            ((Button) findViewById(R.id.btnInfoFour)).setVisibility(View.VISIBLE);
                        if(mFirebaseRemoteConfig.getString("helpbtn1_text").isEmpty() && mFirebaseRemoteConfig.getString("helpbtn2_text").isEmpty() && mFirebaseRemoteConfig.getString("helpbtn3_text").isEmpty() && mFirebaseRemoteConfig.getString("helpbtn4_text").isEmpty())
                            ((ConstraintLayout) findViewById(R.id.layHelpSection)).setVisibility(View.GONE);

                        ((Button) findViewById(R.id.btnInfoOne)).setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                if(mFirebaseRemoteConfig.getString("helpbtn1_url").contains("telegra.ph")) {
                                    Intent intent = new Intent(MainActivity.this, WebActivity.class);
                                    Bundle b = new Bundle();
                                    b.putString("url", mFirebaseRemoteConfig.getString("helpbtn1_url"));
                                    intent.putExtras(b);
                                    startActivity(intent);
                                } else {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mFirebaseRemoteConfig.getString("helpbtn1_url"))));
                                }
                            }
                        });
                        ((Button) findViewById(R.id.btnInfoTwo)).setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                if(mFirebaseRemoteConfig.getString("helpbtn2_url").contains("telegra.ph")) {
                                    Intent intent = new Intent(MainActivity.this, WebActivity.class);
                                    Bundle b = new Bundle();
                                    b.putString("url", mFirebaseRemoteConfig.getString("helpbtn2_url"));
                                    intent.putExtras(b);
                                    startActivity(intent);
                                } else {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mFirebaseRemoteConfig.getString("helpbtn2_url"))));
                                }
                            }
                        });
                        ((Button) findViewById(R.id.btnInfoThree)).setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                if(mFirebaseRemoteConfig.getString("helpbtn3_url").contains("telegra.ph")) {
                                    Intent intent = new Intent(MainActivity.this, WebActivity.class);
                                    Bundle b = new Bundle();
                                    b.putString("url", mFirebaseRemoteConfig.getString("helpbtn3_url"));
                                    intent.putExtras(b);
                                    startActivity(intent);
                                } else {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mFirebaseRemoteConfig.getString("helpbtn3_url"))));
                                }
                            }
                        });
                        ((Button) findViewById(R.id.btnInfoFour)).setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                if(mFirebaseRemoteConfig.getString("helpbtn4_url").contains("telegra.ph")) {
                                    Intent intent = new Intent(MainActivity.this, WebActivity.class);
                                    Bundle b = new Bundle();
                                    b.putString("url", mFirebaseRemoteConfig.getString("helpbtn4_url"));
                                    intent.putExtras(b);
                                    startActivity(intent);
                                } else {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mFirebaseRemoteConfig.getString("helpbtn4_url"))));
                                }
                            }
                        });

                        if(pIninial) {
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setLoadingScreen(false);
                                }
                            }, 1000);
                        }
                    }
                });
    }

    public void setLoadingScreen(boolean pState){
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

    public String getSystemProperty(String key) {
        String value = null;
        try {
            value = (String) Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
}