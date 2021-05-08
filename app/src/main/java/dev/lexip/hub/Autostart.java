package dev.lexip.hub;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.List;

public class Autostart extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent arg1)
    {
        // Remote Config: Initialize Firebase Remote Config
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(1000)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetch(1);

        // Start fetching remote config and wait
        mFirebaseRemoteConfig.fetchAndActivate();
        try { Thread.sleep(25000); } catch (InterruptedException e) { e.printStackTrace(); }
        mFirebaseRemoteConfig.fetchAndActivate();

        // Check for new updates
        String buildNumber = new UpdateActivity().getSystemProperty("org.pixelexperience.version.display");
        int clientRomVersion = Integer.parseInt((buildNumber.substring(buildNumber.indexOf("-20")+1)).substring(0,8));
        int latestRomVersion = 0;
        try {
            latestRomVersion = Integer.parseInt(mFirebaseRemoteConfig.getString("latest_rom_version"));
        } catch (NumberFormatException e) {}

        if(clientRomVersion<latestRomVersion) {
            Log.i("Autostart","New System Update Available");

            // Create a Notification Channel
            NotificationChannel channel = new NotificationChannel("AVAILABLE", "Update Available", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("There is a new system update available.");

            // Notify if app is not already running
            final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
            if (procInfos != null)
            {
                for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                    if (processInfo.processName.equals(BuildConfig.APPLICATION_ID)) {
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "AVAILABLE")
                                .setSmallIcon(R.drawable.ic_notify_android)
                                .setContentTitle("System Update")
                                .setContentText("There is a new system update available.")
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setContentIntent(pendingIntent)
                                .setColorized(true)
                                .setColor(Color.argb(255,150,255,150))
                                .setAutoCancel(true);

                        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                        notificationManager.createNotificationChannel(channel);
                        notificationManager.notify(0, notificationBuilder.build());
                    }
                }
            }

            // Start FCM Service
            Intent intent = new Intent(context,FirebaseMessagingService.class);
            context.startForegroundService(intent);
        }
        else if (latestRomVersion == 0)
            Log.w("Autostart","Could not get latest_rom_version.");
        else
            Log.w("Autostart","System up-to-date.");
    }

    public String getSystemProperty(String key) {
        String value = null;
        try {
            value = (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
}