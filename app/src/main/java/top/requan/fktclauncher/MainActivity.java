package top.requan.fktclauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import top.requan.fktclauncher.util.OverlayPermissionUtils;

public class MainActivity extends AppCompatActivity {

    private static final long LONG_PRESS_DURATION = 5000; // 长按时间5秒
    private static final int TAP_COUNT_THRESHOLD = 5; // 点击次数阈值
    private static final long TAP_TIME_WINDOW = 1000; // 1秒的时间窗口
    private static final int SWIPE_THRESHOLD = 100;
    private static final int FLOATING_WINDOW_HEIGHT = 100;
    private static final int SWIPE_Y_THRESHOLD = 250;

    private final Handler handler = new Handler();
    private boolean isLongPress = false;
    private boolean isLongPressInProgress = false;
    private final List<Long> tapTimes = new ArrayList<>();

    private TextView timeTextView;
    private TextView dateTextView;
    //private TextView versionTextView;
    private PackageManager packageManager;

    private LinearLayout floatingWindow;

    private boolean isStatusBarDisabled = false;

    private float startX;
    private float startY;
    private float endX;
    private float endY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeTextView = findViewById(R.id.time_text);
        dateTextView = findViewById(R.id.date_text);
        //versionTextView = findViewById(R.id.version_text);
        View mainView = findViewById(R.id.main_view);

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        ScreenOffReceiver screenOffReceiver = new ScreenOffReceiver();
        registerReceiver(screenOffReceiver, filter);

        Intent serviceIntent = new Intent(this, ScreenService.class);
        startService(serviceIntent);

        while (!OverlayPermissionUtils.isOverlayPermissionGranted(this)) {
            OverlayPermissionUtils.requestOverlayPermission(this);
        }



        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 什么都不做，禁用返回键
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        packageManager = getPackageManager();


        mainView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        if (!isLongPressInProgress) {
                            isLongPressInProgress = true;
                            handler.postDelayed(longPressRunnable, LONG_PRESS_DURATION);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isLongPressInProgress) {
                            handler.removeCallbacks(longPressRunnable);
                            if (!isLongPress) {
                                handleTap();
                            }
                            isLongPressInProgress = false;

                            endX = event.getX();
                            endY = event.getY();
                            float deltaX = endX - startX;
                            float deltaY = endY - startY;

                            if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > SWIPE_THRESHOLD) {
                                if (deltaX > 0) {
                                    // 向右滑动
                                    openApp("com.hsc.walk");
                                } else if (deltaX < 0) {
                                    // 向左滑动
                                    openApp("com.hsc.heartrate");
                                }
                            }
                        }
                        break;
                }
                return true;
            }
        });

        // Update the time every second
        handler.postDelayed(updateTimeRunnable, 0);
    }


    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateTime();
            handler.postDelayed(this, 1000);
        }
    };

    private void updateTime() {
        long currentTimeMillis = System.currentTimeMillis();
        String timeText = DateFormat.format("HH:mm:ss", currentTimeMillis).toString();
        String dateText = DateFormat.format("yyyy-MM-dd", currentTimeMillis).toString();

        timeTextView.setText(timeText);
        dateTextView.setText(dateText);

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            String versionText = "Version: " + packageInfo.versionName;
            //versionTextView.setText(versionText);
        } catch (PackageManager.NameNotFoundException e) {
            //versionTextView.setText("Version: Unknown");
        }
    }

    private void handleTap() {
        long currentTime = SystemClock.elapsedRealtime();
        tapTimes.add(currentTime);

        // Remove old tap times outside of the time window
        while (!tapTimes.isEmpty() && (currentTime - tapTimes.get(0)) > TAP_TIME_WINDOW) {
            tapTimes.remove(0);
        }

        if (tapTimes.size() >= TAP_COUNT_THRESHOLD) {
            openOriginalLauncher();
            tapTimes.clear(); // Reset tap times after successful launch
        }
    }

    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            isLongPress = true;
            handleLongPress();
        }
    };

    private void handleLongPress() {
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > SWIPE_Y_THRESHOLD) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        isLongPress = false; // Reset flag after handling
    }

    private void openOriginalLauncher() {
        String defaultLauncherPackage = getDefaultLauncherPackage();
        if (defaultLauncherPackage != null) {
//            PackageManager packageManager = getPackageManager();
            Intent intent = new Intent();
            intent.setPackage(defaultLauncherPackage);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
//            Toast.makeText(MainActivity.this, intent.toString(), Toast.LENGTH_SHORT).show();

            // 设置 FLAG_ACTIVITY_NEW_TASK 以确保新任务启动
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开原始启动器", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getDefaultLauncherPackage() {
        SharedPreferences preferences = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getString(SettingsActivity.KEY_DEFAULT_LAUNCHER, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //blockNav();
        if (isSystemApp(this) && !isStatusBarDisabled) {
            disableStatusBar(this);
            isStatusBarDisabled = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //restoreNav();
        if (isStatusBarDisabled) {
            enableStatusBar(this);
            isStatusBarDisabled = false;
        }
    }

    public static void disableStatusBar(Context context) {
        Log.d(MainActivity.class.getCanonicalName(), "disableStatusBar: ");
        // Read from property or pass it in function, whatever works for you!
        boolean disable = true;
        @SuppressLint("WrongConstant") Object statusBarService = context.getSystemService(Context.STATUS_BAR_SERVICE);

        Class<?> statusBarManager;
        try {
            statusBarManager = Class.forName("android.app.StatusBarManager");
            try {
                final Method disableStatusBarFeatures = statusBarManager.getMethod("disable", int.class);
                disableStatusBarFeatures.setAccessible(true);
                if (disable) {
                    disableStatusBarFeatures.invoke(statusBarService, 0x00010000 | 0x00040000); // Disable EXPAND and NOTIFICATION_ALERTS
                } else {
                    disableStatusBarFeatures.invoke(statusBarService, 0x00000000); // Re-enable everything
                }
            } catch (Exception e) {
                Log.e(MainActivity.class.getCanonicalName(), "disableStatusBar: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            Log.e(MainActivity.class.getCanonicalName(), "disableStatusBar: " + e.getMessage(), e);
        }
    }

    public static void enableStatusBar(Context context) {
        Log.d(MainActivity.class.getCanonicalName(), "enableStatusBar: ");
        @SuppressLint("WrongConstant") Object statusBarService = context.getSystemService(Context.STATUS_BAR_SERVICE);

        Class<?> statusBarManager;
        try {
            statusBarManager = Class.forName("android.app.StatusBarManager");
            try {
                final Method disableStatusBarFeatures = statusBarManager.getMethod("disable", int.class);
                disableStatusBarFeatures.setAccessible(true);
                disableStatusBarFeatures.invoke(statusBarService, 0x00000000); // Re-enable everything
            } catch (Exception e) {
                Log.e(MainActivity.class.getCanonicalName(), "enableStatusBar: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            Log.e(MainActivity.class.getCanonicalName(), "enableStatusBar: " + e.getMessage(), e);
        }
    }

    public boolean isSystemApp(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
            return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openApp(String packageName) {
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            startActivity(intent);
        }
    }

    @Deprecated
    private void blockNav() {
        Toast.makeText(MainActivity.this, "LOADING FLOATING WINDOW", Toast.LENGTH_SHORT).show();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // 创建一个 LinearLayout 作为浮动窗口的根布局
        floatingWindow = new LinearLayout(this);
        floatingWindow.setBackgroundColor(0x00000000); // 半透明黑色背景

        // 设置 WindowManager.LayoutParams
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, // 宽度为屏幕宽度
                FLOATING_WINDOW_HEIGHT, // 高度为自定义的变量
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );

        // 设置窗口的位置在屏幕的底部
        params.gravity = Gravity.BOTTOM;

        // 添加浮动窗口到 WindowManager
        windowManager.addView(floatingWindow, params);
    }

    @Deprecated
    private void restoreNav() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null && floatingWindow != null) {
            windowManager.removeView(floatingWindow);
        }
    }
}


//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        Log.d("tag", "window focus changed");
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            collapseNow();
//        }
//    }
//
//    public void collapseNow() {
//
//        try {
//            // Initialize 'collapseNotificationHandler'
//            if (collapseNotificationHandler == null) {
//                collapseNotificationHandler = new Handler();
//            }
//
//            // Post a Runnable with some delay - currently set to 300 ms
//            collapseNotificationHandler.postDelayed(new Runnable() {
//
//                @Override
//                public void run() {
//
//                    // Use reflection to trigger a method from 'StatusBarManager'
//                    @SuppressLint("WrongConstant") Object statusBarService = getSystemService("statusbar");
//                    Class<?> statusBarManager = null;
//
//                    try {
//                        statusBarManager = Class.forName("android.app.StatusBarManager");
//                    } catch (ClassNotFoundException e) {
//                        e.printStackTrace();
//                    }
//
//                    Method collapseStatusBar = null;
//                    try {
//                        // Prior to API 17, the method to call is 'collapse()'
//                        // API 17 onwards, the method to call is `collapsePanels()`
//                        if (Build.VERSION.SDK_INT > 16) {
//                            collapseStatusBar = statusBarManager.getMethod("collapsePanels");
//                        } else {
//                            collapseStatusBar = statusBarManager.getMethod("collapse");
//                        }
//                    } catch (NoSuchMethodException e) {
//                        e.printStackTrace();
//                    }
//
//                    collapseStatusBar.setAccessible(true);
//
//                    try {
//                        collapseStatusBar.invoke(statusBarService);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    } catch (IllegalAccessException e) {
//                        e.printStackTrace();
//                    } catch (InvocationTargetException e) {
//                        e.printStackTrace();
//                    }
//                    // Currently, the delay is 10 ms. You can change this
//                    // value to suit your needs.
//                    collapseNotificationHandler.postDelayed(this, 10L);
//                }
//            }, 10L);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }