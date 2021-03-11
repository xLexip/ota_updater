package dev.lexip.hub;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Scanner;

public class UpdateActivity extends AppCompatActivity {

    // Progress Dialog
    private Context context;

    private boolean downloading = false;
    private boolean readyForFlashing;

    private String magiskURL;
    private String updateURL;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_update);
        context = UpdateActivity.this;
        ((Switch) findViewById(R.id.switchKeepRoot)).setChecked(false);
        ((Switch) findViewById(R.id.switchKeepRoot)).setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!downloading){
            ((Button) findViewById(R.id.btnFlash)).setVisibility(View.VISIBLE);
        }

        // Remote Config: Initialize Firebase Remote Config
        FirebaseRemoteConfig mFirebaseRemoteConfig;
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(1000)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetch(1);

        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        ((TextView) findViewById(R.id.tvAppName)).setText(mFirebaseRemoteConfig.getString("app_name"));
                        ((TextView) findViewById(R.id.tvRomName)).setText(mFirebaseRemoteConfig.getString("rom_name"));
                        ((TextView) findViewById(R.id.tvMaintenanceType)).setText(mFirebaseRemoteConfig.getString("maintenance_type"));
                        ((TextView) findViewById(R.id.tvVersion)).setText(mFirebaseRemoteConfig.getString("latest_rom_version_title"));
                    }
                });

        ((Switch) findViewById(R.id.switchAutoInstall)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    if (!hasRootAccess()) {
                        ((Switch) findViewById(R.id.switchAutoInstall)).setChecked(false);
                        Toast.makeText(UpdateActivity.this, "E: No root access",
                                Toast.LENGTH_SHORT).show();
                        ((Switch) findViewById(R.id.switchKeepRoot)).setVisibility(View.GONE);
                        ((Switch) findViewById(R.id.switchKeepRoot)).setChecked(false);
                        ((Button) findViewById(R.id.btnFlash)).setText("UPDATE MANUALLY");
                        return;
                    }
                    ((Switch) findViewById(R.id.switchKeepRoot)).setVisibility(View.VISIBLE);
                    ((Switch) findViewById(R.id.switchKeepRoot)).setChecked(true);
                    ((Button) findViewById(R.id.btnFlash)).setText("UPDATE NOW");
                } else {
                    ((Switch) findViewById(R.id.switchKeepRoot)).setVisibility(View.GONE);
                    ((Switch) findViewById(R.id.switchKeepRoot)).setChecked(false);
                    ((Button) findViewById(R.id.btnFlash)).setText("UPDATE MANUALLY");
                }
            }
        });

        ((Button) findViewById(R.id.btnFlash)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((Button) findViewById(R.id.btnFlash)).getText().equals("REBOOT NOW")){
                    flash();
                    return;
                }
                ((Button) findViewById(R.id.btnFlash)).setVisibility(View.INVISIBLE);

                ((Switch) findViewById(R.id.switchAutoInstall)).setActivated(false);
                ((Switch) findViewById(R.id.switchKeepRoot)).setActivated(false);

                if(((Switch) findViewById(R.id.switchKeepRoot)).isChecked()){
                    magiskURL = mFirebaseRemoteConfig.getString("magisk_url");
                    if(getSystemProperty("org.pixelexperience.device").equals("dumpling"))
                        updateURL =  mFirebaseRemoteConfig.getString("dumpling_download");
                    else if(getSystemProperty("org.pixelexperience.device").equals("cheeseburger"))
                        updateURL =  mFirebaseRemoteConfig.getString("cheeseburger_download");

                    Toast.makeText(UpdateActivity.this, "This is a BETA feature.\nDownloading all required files...",
                            Toast.LENGTH_LONG).show();
                    Toast.makeText(UpdateActivity.this, "This is a BETA feature.\nDownloading all required files...",
                            Toast.LENGTH_LONG).show();
                    Toast.makeText(UpdateActivity.this, "This can take a while, depending on your location and connection. The device will automatically reboot after downloading.",
                            Toast.LENGTH_LONG).show();
                    Toast.makeText(UpdateActivity.this, "This can take a while, depending on your location and connection. The device will automatically reboot after downloading.",
                            Toast.LENGTH_LONG).show();
                    Toast.makeText(UpdateActivity.this, "This can take a while, depending on your location and connection. The device will automatically reboot after downloading.",
                            Toast.LENGTH_LONG).show();

                    new Thread(){
                        public void run(){
                            new DownloadFileFromURL().execute(updateURL);

                            while(downloading) {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            if(((Switch) findViewById(R.id.switchKeepRoot)).isChecked()) {
                                new DownloadFileFromURL().execute(mFirebaseRemoteConfig.getString("magisk_url"));
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                while (downloading) {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            UpdateActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    ((Button) findViewById(R.id.btnFlash)).setVisibility(View.VISIBLE);
                                    ((Button) findViewById(R.id.btnFlash)).setText("REBOOT NOW");

                                    ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(2500);
                                    Toast.makeText((Context) UpdateActivity.this, "Rebooting to update in 30 seconds...",
                                            Toast.LENGTH_LONG).show();
                                    Toast.makeText((Context) UpdateActivity.this, "Rebooting to update in 30 seconds...",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                            try {
                                Thread.sleep(20000);
                                UpdateActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(10);
                                        Toast.makeText((Context) UpdateActivity.this, "Rebooting to update in 10 seconds...",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                                Thread.sleep(9900);
                                flash();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();

                } else {
                    ((Button) findViewById(R.id.btnFlash)).setVisibility(View.VISIBLE);
                    Intent intent = new Intent(UpdateActivity.this, WebActivity.class);
                    Bundle b = new Bundle();
                    b.putString("url", "https://telegra.ph/How-to-update-Pixel-Experience-12-05");
                    intent.putExtras(b);
                    startActivity(intent);

                    new Thread(){
                        public void run(){
                            try {
                                Thread.sleep(3500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if(getSystemProperty("org.pixelexperience.device").equals("dumpling"))
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mFirebaseRemoteConfig.getString("dumpling_download"))));
                            else if(getSystemProperty("org.pixelexperience.device").equals("cheeseburger"))
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mFirebaseRemoteConfig.getString("cheeseburger_download"))));
                        }
                    }.start();
                }
            }
        });
    }

    public void flash(){
        Process p = null;
        try {
            Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","rm /cache/recovery/command"});
            Thread.sleep(50);
            Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","echo 'boot-recovery ' > /cache/recovery/command"});
            Thread.sleep(50);
            Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","echo '--update_package=/data/data/dev.lexip.hub/files/"+updateURL.substring(updateURL.length()-10).replace("/","")+".zip"+"' >> /cache/recovery/command"});
            Thread.sleep(50);

            if(((Switch) findViewById(R.id.switchKeepRoot)).isChecked())
                Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","echo '--update_package=/data/data/dev.lexip.hub/files/"+magiskURL.substring(magiskURL.length()-10).replace("/","")+".zip"+"' >> /cache/recovery/command"});

            Thread.sleep(50);
            Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","echo '--wipe_cache' >> /cache/recovery/command"});
            Thread.sleep(50);
            Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","echo 'reboot' >> /cache/recovery/command"});
        } catch (IOException | InterruptedException e) {
            Log.i("UpdateActivity", "Update failed", e);
        }

        try {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","reboot recovery"});
        } catch (Exception ex) {
            Log.i("UpdateActivity", "Reboot failed", ex);
        }

    }


    /**
     * Background Async Task to download file
     */
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            downloading = true;
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection connection = url.openConnection();
                connection.connect();

                int lenghtOfFile = connection.getContentLength();

                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                OutputStream output = context.openFileOutput((url.toString()).substring((url.toString()).length()-10).replace("/","")+".zip", Context.MODE_PRIVATE);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;

                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    output.write(data, 0, count);
                }

                output.flush();

                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("DownloadError: ", e.getMessage());
            }

            return null;
        }

        protected void onProgressUpdate(String... progress) {
            ((ProgressBar) findViewById(R.id.progressBar)).setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String file_url) {
            downloading = false;
        }
    }

    /**
     *  Returns if the app has superuser rights
     */
    public boolean hasRootAccess() {
        try {
            java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","cd / && ls"}).getInputStream()).useDelimiter("\\A");
            return !(s.hasNext() ? s.next() : "").equals("");
        } catch (IOException e) {
            e.printStackTrace();
        }
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