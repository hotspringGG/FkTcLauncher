package top.requan.fktclauncher.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

public class OverlayPermissionUtils {

    // Check if the overlay permission is granted
    public static boolean isOverlayPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        } else {
            // For older versions, assume permission is granted
            return true;
        }
    }

    // Request the overlay permission from the user
    public static void requestOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isOverlayPermissionGranted(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                // Use FLAG_ACTIVITY_NEW_TASK to avoid needing a result
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Toast.makeText(context, "Please grant overlay permission", Toast.LENGTH_LONG).show();
            }
        }
    }
}
