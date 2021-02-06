package dev.lexip.hub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class Autostart extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent arg1)
    {
        Intent intent = new Intent(context,FirebaseMessagingService.class);
        context.startForegroundService(intent);
        Log.i("Autostart", "started");
    }
}