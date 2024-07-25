package top.requan.fktclauncher;

import android.annotation.SuppressLint;
import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

//import java.util.List;

public class MainActivity extends AppCompatActivity {

//    private static final String TARGET_PACKAGE_1 = "com.example.app1";
//    private static final String TARGET_PACKAGE_2 = "com.example.app2";
    private static final long LONG_PRESS_DURATION = 3000; // 3 seconds
    private int clickCount = 0;
    private final Handler handler = new Handler();
    private final Runnable resetClickCount = new Runnable() {
        @Override
        public void run() {
            clickCount = 0;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // 启用深色模式
        setContentView(R.layout.activity_main);

        findViewById(R.id.launch_button).setOnTouchListener(new View.OnTouchListener() {
            private boolean isLongPress = false;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isLongPress = false;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (isLongPress) return;
                                isLongPress = true;
                                // Long press detected
                                openSettings();
                            }
                        }, LONG_PRESS_DURATION);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isLongPress) {
                            clickCount++;
                            handler.removeCallbacks(resetClickCount);
                            handler.postDelayed(resetClickCount, 1000); // 1 second to reset click count
                            if (clickCount >= 5) {
                                goToHomeLauncher();
                            }
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    private void launchTargetApp(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "应用未安装", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHomeLauncher() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    private void openSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_HOME_SETTINGS);
        startActivity(intent);
    }
}


