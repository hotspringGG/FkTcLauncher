package top.requan.fktclauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final long LONG_PRESS_DURATION = 5000; // 长按时间5秒
    private static final int TAP_COUNT_THRESHOLD = 5; // 点击次数阈值
    private static final long TAP_TIME_WINDOW = 1000; // 1秒的时间窗口

    private final Handler handler = new Handler();
    private boolean isLongPress = false;
    private boolean isLongPressInProgress = false;
    private final List<Long> tapTimes = new ArrayList<>();

    private TextView timeTextView;
    private PackageManager packageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeTextView = findViewById(R.id.time_text);
        View mainView = findViewById(R.id.main_view);

        packageManager = getPackageManager();

        mainView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
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
        timeTextView.setText(timeText);
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
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        isLongPress = false; // Reset flag after handling
    }

    private void openOriginalLauncher() {
        String defaultLauncherPackage = getDefaultLauncherPackage();
        if (defaultLauncherPackage != null) {
            Intent intent = packageManager.getLaunchIntentForPackage(defaultLauncherPackage);
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "无法找到选择的启动器", Toast.LENGTH_SHORT).show();
            }
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
}
