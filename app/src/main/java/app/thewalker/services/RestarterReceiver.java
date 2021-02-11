package app.thewalker.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class RestarterReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("Broadcast Listened", "Service tried to stop");
        Toast.makeText(context, "Service restarted", Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, RunningService.class));
        } else {
            context.startService(new Intent(context, RunningService.class));
        }
    }
    }

