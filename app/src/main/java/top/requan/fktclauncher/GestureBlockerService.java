package top.requan.fktclauncher;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

public class GestureBlockerService extends AccessibilityService {

    private static final String TAG = "GestureBlockerService";
    private static final String LAUNCHER_PACKAGE_NAME = "top.requan.fktclauncher"; // 替换为你的 launcher 包名

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 获取当前前台应用的包名
        String currentForegroundPackageName = getForegroundPackageName();

        // 仅当当前应用为你的 launcher 时处理事件
        if (LAUNCHER_PACKAGE_NAME.equals(currentForegroundPackageName)) {
            int eventType = event.getEventType();
            Log.d(TAG, "Event Type: " + eventType);

            // 示例: 处理控制中心事件
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                String packageName = event.getPackageName().toString();
                Log.d(TAG, "Package Name: " + packageName);

                if (packageName.equals("com.android.systemui")) {
                    Log.d(TAG, "Intercepted system UI event");
                    // 这里可以执行相关的拦截或处理逻辑
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
//        Log.d(TAG, "Accessibility Service Interrupted");
    }

    private String getForegroundPackageName() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        if (runningAppProcesses != null && !runningAppProcesses.isEmpty()) {
            for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return processInfo.processName;
                }
            }
        }
        return null;
    }
}
