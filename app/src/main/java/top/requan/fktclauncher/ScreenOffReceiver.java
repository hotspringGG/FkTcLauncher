package top.requan.fktclauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenOffReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(homeIntent);
        }
    }
}
