package dev.lexip.hub;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class UpdateActivity extends AppCompatActivity {

    private Context context;
    private boolean downloading = false;
    private String updateURL;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_update);
        context = UpdateActivity.this;

        ((Switch) findViewById(R.id.switchFlashMagisk)).setChecked(true);
        ((Switch) findViewById(R.id.switchFlashMagisk)).setVisibility(View.VISIBLE);

        ((Button) findViewById(R.id.btnChangelog)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(UpdateActivity.this, WebActivity.class);
                Bundle b = new Bundle();
                b.putString("url", "https://telegra.ph/Changelog-12-14");
                intent.putExtras(b);
                startActivity(intent);
            }
        });

        // Delete old update files
        if(new File("rom-package.zip").exists())
            new File("rom-package.zip").delete();
        if(new File("magisk.zip").exists())
            new File("magisk.zip").delete();
        if(new File("unknown.zip").exists())
            new File("unknown.zip").delete();
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

        ((Button) findViewById(R.id.btnFlash)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((Button) findViewById(R.id.btnFlash)).getText().equals("REBOOT NOW")){
                    flash();
                    return;
                }
                else if(((Button) findViewById(R.id.btnFlash)).getText().equals("CANCEL UPDATE")){
                    Intent intent = new Intent(UpdateActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }

                // Request battery optimization ignoring
                Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }

                ((Button) findViewById(R.id.btnFlash)).setVisibility(View.INVISIBLE);
                ((Switch) findViewById(R.id.switchFlashMagisk)).setVisibility(View.INVISIBLE);

                if(getSystemProperty("org.pixelexperience.device").equals("dumpling"))
                    updateURL =  mFirebaseRemoteConfig.getString("dumpling_download");
                else if(getSystemProperty("org.pixelexperience.device").equals("cheeseburger"))
                    updateURL =  mFirebaseRemoteConfig.getString("cheeseburger_download");

                Toast.makeText(UpdateActivity.this, "Downloading all required files...",
                        Toast.LENGTH_LONG).show();
                Toast.makeText(UpdateActivity.this, "Downloading all required files...",
                        Toast.LENGTH_LONG).show();
                Toast.makeText(UpdateActivity.this, "This can take a while. The device will automatically reboot after downloading.",
                        Toast.LENGTH_LONG).show();
                Toast.makeText(UpdateActivity.this, "This can take a while. The device will automatically reboot after downloading.",
                        Toast.LENGTH_LONG).show();
                Toast.makeText(UpdateActivity.this, "This can take a while. The device will automatically reboot after downloading.",
                        Toast.LENGTH_LONG).show();


                new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(19500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        UpdateActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                ((Button) findViewById(R.id.btnFlash)).setVisibility(View.VISIBLE);
                                ((Button) findViewById(R.id.btnFlash)).setText("CANCEL UPDATE");
                                ((Switch) findViewById(R.id.switchFlashMagisk)).setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }.start();

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

                        if(((Switch) findViewById(R.id.switchFlashMagisk)).isChecked()) {
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

                                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(2000);
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
                                    ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200);
                                    Toast.makeText((Context) UpdateActivity.this, "Rebooting to update in 10 seconds...",
                                            Toast.LENGTH_LONG).show();
                                    Toast.makeText((Context) UpdateActivity.this, "You may need to enter ypur pincode or pattern to decrypt and update.",
                                            Toast.LENGTH_LONG).show();
                                    Toast.makeText((Context) UpdateActivity.this, "You may need to enter ypur pincode or pattern to decrypt and update.",
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
            }
        });
    }

    public void flash(){
        try {
            File cmdFile = new File("/cache/recovery/command");
            if(cmdFile.exists() && cmdFile.isFile())
                cmdFile.delete();

            FileWriter myWriter = new FileWriter("/cache/recovery/command");
            myWriter.write("boot-recovery\n--update_package=/data/data/dev.lexip.hub/files/rom-package.zip\n");
            if(((Switch) findViewById(R.id.switchFlashMagisk)).isChecked())
                myWriter.write("--update_package=/data/data/dev.lexip.hub/files/magisk.zip\n");
            myWriter.write("--wipe_cache\nreboot");
            myWriter.close();

            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.reboot("recovery");
            Toast.makeText((Context) UpdateActivity.this, "Thanks Android, but no, this is NOT a factory reset.",
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.i("UpdateActivity", "Update failed", e);
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

                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                OutputStream output;
                if(url.toString().toLowerCase().contains("pixelexperience"))
                    output = context.openFileOutput("rom-package.zip", Context.MODE_PRIVATE);
                else if(url.toString().toLowerCase().contains("magisk"))
                    output = context.openFileOutput("magisk.zip", Context.MODE_PRIVATE);
                else
                    output = context.openFileOutput("unknown.zip", Context.MODE_PRIVATE);

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