package dev.lexip.hub;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Stack;

public class UpdateActivity extends AppCompatActivity {

    private Context context;
    private boolean downloading = false;
    private String updateURL;
    private NotificationChannel channel;
    private BroadcastReceiver receiver;
    private Stack<Long> downloads;
    private String config;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_update);
        context = UpdateActivity.this;

        ((Switch) findViewById(R.id.switchFlashMagisk)).setChecked(true);
        ((Switch) findViewById(R.id.switchFlashMagisk)).setVisibility(View.VISIBLE);

        // Delete old update files
        File dir = new File(Environment.DIRECTORY_DOWNLOADS+"hub");
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }

        // Create the NotificationChannel
        channel = new NotificationChannel("Updating", "System Update", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Processing a system update.");

        refreshConfig();
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

        // Declare Listeners
        ((Button) findViewById(R.id.btnChangelog)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(UpdateActivity.this, WebActivity.class);
                Bundle b = new Bundle();
                b.putString("url", "https://telegra.ph/Changelog-12-14");
                intent.putExtras(b);
                startActivity(intent);
            }
        });

        ((Switch)findViewById(R.id.switchFlashMagisk)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                refreshConfig();
                try {
                    if(isChecked && config.contains("flash_magisk=false")) {
                        String[] tmp = config.split("flash_magisk=false");
                        FileWriter writer = null;
                        writer = new FileWriter("config");
                        writer.write(Arrays.toString(tmp)+"flash_magisk=true");
                        writer.close();
                    } else if(!isChecked && config.contains("flash_magisk=true")){
                        String[] tmp = config.split("flash_magisk=true");
                        FileWriter writer = null;
                        writer = new FileWriter("config");
                        writer.write(Arrays.toString(tmp)+"flash_magisk=false");
                        writer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
                else if(((Button) findViewById(R.id.btnFlash)).getText().equals("CANCEL UPDATE")){
                    Intent mStartActivity = new Intent(context, MainActivity.class);
                    int mPendingIntentId = 123456;
                    PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                    System.exit(0);
                    return;
                }

                // Request to ignore the battery optimization
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

                Toast.makeText(UpdateActivity.this, "Downloading update files...",
                        Toast.LENGTH_LONG).show();
                Toast.makeText(UpdateActivity.this, "Downloading update files...",
                        Toast.LENGTH_LONG).show();
                Toast.makeText(UpdateActivity.this, "This can take a while, your device will automatically reboot after downloading.",
                        Toast.LENGTH_LONG).show();
                Toast.makeText(UpdateActivity.this, "This can take a while, your device will automatically reboot after downloading.",
                        Toast.LENGTH_LONG).show();
                Toast.makeText(UpdateActivity.this, "This can take a while, your device will automatically reboot after downloading.",
                        Toast.LENGTH_LONG).show();

                // Show Magisk Switch and cancelling option after the toasts have been shown
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

                // Download Thread
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

                        // Inform the user that the device will reboot soon
                        UpdateActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                ((Button) findViewById(R.id.btnFlash)).setVisibility(View.VISIBLE);
                                ((Button) findViewById(R.id.btnFlash)).setText("REBOOT NOW");

                                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(2000);
                                Toast.makeText((Context) UpdateActivity.this, "Rebooting in 30 seconds...",
                                        Toast.LENGTH_LONG).show();
                                Toast.makeText((Context) UpdateActivity.this, "Rebooting in 30 seconds...",
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
                                    Toast.makeText((Context) UpdateActivity.this, "You may need to confirm the update by entering your screen lock.",
                                            Toast.LENGTH_LONG).show();
                                    Toast.makeText((Context) UpdateActivity.this, "You may need to confirm the update by entering your screen lock.",
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

    /**
     * Creates the recovery command and reboots to recovery
     */
    public void flash(){
        try {
            File cmdFile = new File("/cache/recovery/command");
            if(cmdFile.exists() && cmdFile.isFile())
                cmdFile.delete();

            FileWriter myWriter = new FileWriter("/cache/recovery/command");
            myWriter.write("boot-recovery\n--update_package=/data/data/dev.lexip.hub/files/rom-package.zip\n");
            refreshConfig();
            if(config.contains("flash_magisk=true"))
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

        /**
         * Updates the progress bar
         * @param progress
         */
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

    public void refreshConfig() {
        try {
            if (!new File("config").exists()) {
                FileWriter writer = null;
                writer = new FileWriter("config");
                writer.write("flash_magisk=true");
                writer.close();

            } else {
                File myObj = new File("config");
                Scanner reader = new Scanner(myObj);
                while (reader.hasNextLine()) {
                    config += reader.nextLine();
                }
                reader.close();

                if (!config.contains("flash_magisk")) {
                    FileWriter writer = null;
                    writer = new FileWriter("config");
                    writer.write("flash_magisk=true");
                    writer.close();
                }
            }
        } catch(IOException e) {}
    }
}