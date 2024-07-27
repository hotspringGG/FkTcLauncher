package top.requan.fktclauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "LauncherPrefs";
    public static final String KEY_DEFAULT_LAUNCHER = "default_launcher";

    private ListView listView;
    private PackageManager packageManager;
    private List<String> launcherNames;
    private List<String> launcherPackages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        listView = findViewById(R.id.launcher_list);
        packageManager = getPackageManager();
        launcherNames = new ArrayList<>();
        launcherPackages = new ArrayList<>();

        loadLaunchers();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, launcherNames);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedPackage = launcherPackages.get(position);
                saveDefaultLauncher(selectedPackage);
                Toast.makeText(SettingsActivity.this, "默认启动器已设置:" + selectedPackage.toString(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadLaunchers() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        @SuppressLint("QueryPermissionsNeeded") List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
        String currentPackageName = getPackageName();  // 获取当前应用的包名

        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.activityInfo.packageName;
            if (!packageName.equals(currentPackageName)) {  // 过滤掉当前应用
                String appName = resolveInfo.loadLabel(packageManager).toString();
                launcherNames.add(appName);
                launcherPackages.add(packageName);
            }
        }
    }

    private void saveDefaultLauncher(String packageName) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DEFAULT_LAUNCHER, packageName);
        editor.apply();
    }
}
