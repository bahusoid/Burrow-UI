package com.hamsterbase.burrowui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hamsterbase.burrowui.service.AppInfo;
import com.hamsterbase.burrowui.service.AppManagementService;

import java.io.File;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private TextView timeTextView;
    private TextView dateTextView;
    private TextView amPmTextView;
    private LinearLayout appLinearLayout;
    private List<AppInfo> allApps;
    private List<SettingsManager.SelectedItem> selectedItems;
    private SettingsManager settingsManager;
    private AppManagementService appManagementService;
    private Handler handler;
    private Runnable updateTimeRunnable;
    private BroadcastReceiver batteryReceiver;
    private String batteryText = "";
    private final Map<String, AppInfo> appLookup = new HashMap<>();
    private SimpleDateFormat time24Format;
    private SimpleDateFormat time12Format;
    private SimpleDateFormat amPmFormat;
    private SimpleDateFormat dateFormat;
    private String lastRenderedTime = "";
    private String lastRenderedDate = "";
    private String lastRenderedAmPm = "";

    private float touchStartY;
    private static final float SWIPE_THRESHOLD = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        timeTextView = findViewById(R.id.timeTextView);
        dateTextView = findViewById(R.id.dateTextView);
        amPmTextView = findViewById(R.id.amPmTextView);
        appLinearLayout = findViewById(R.id.appLinearLayout);

        ScrollView appList = findViewById(R.id.appScrollList);
        appList.setOverScrollMode(View.OVER_SCROLL_NEVER);
        appList.setVerticalScrollBarEnabled(false);

        TextView debugTextView = findViewById(R.id.debugTextView);
        if (BuildConfig.DEBUG) {
            debugTextView.setVisibility(View.VISIBLE);
            String debugInfo = "Debug: " + Build.MODEL + " - " + Build.VERSION.RELEASE;
            debugTextView.setText(debugInfo);
        } else {
            debugTextView.setVisibility(View.GONE);
        }

        settingsManager = new SettingsManager(this);
        appManagementService = AppManagementService.getInstance(this);

        handler = new Handler(Looper.getMainLooper());
        initTimeFormatters();
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                long now = System.currentTimeMillis();
                long delayToNextMinute = 60000 - (now % 60000);
                handler.postDelayed(this, delayToNextMinute + 50);
            }
        };

        loadApps();
        displaySelectedApps();

        View rootView = findViewById(android.R.id.content);

        rootView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isLongPress = false;
            private Handler longPressHandler = new Handler();
            private static final long LONG_PRESS_TIMEOUT = 600;
            private boolean isPullDownEnabled = false;

            private Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    isLongPress = true;
                    openSettingsActivity();
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchStartY = event.getY();
                        isLongPress = false;
                        isPullDownEnabled = touchStartY > 50 && settingsManager.isEnablePullDownSearch();
                        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (isPullDownEnabled && Math.abs(event.getY() - touchStartY) > SWIPE_THRESHOLD) {
                            longPressHandler.removeCallbacks(longPressRunnable);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        if (isLongPress) {
                            return true;
                        }
                        float touchEndY = event.getY();
                        float deltaY = touchEndY - touchStartY;
                        if (isPullDownEnabled && deltaY > SWIPE_THRESHOLD) {
                            openSearchActivity();
                            return true;
                        }
                        return false;

                    case MotionEvent.ACTION_CANCEL:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        return false;
                }
                return false;
            }
        });

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateBatteryStatus(intent);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        initTimeFormatters();
        loadApps();
        displaySelectedApps();
        handler.post(updateTimeRunnable);

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTimeRunnable);
        unregisterReceiver(batteryReceiver);
    }

    private void updateTime() {
        Date now = new Date();
        if (settingsManager.isUse24HourFormat()) {
            String currentTime = time24Format.format(now);
            if (!currentTime.equals(lastRenderedTime)) {
                timeTextView.setText(currentTime);
                lastRenderedTime = currentTime;
            }
            if (amPmTextView.getVisibility() != View.GONE) {
                amPmTextView.setVisibility(View.GONE);
            }
        } else {
            String currentTime = time12Format.format(now);
            if (!currentTime.equals(lastRenderedTime)) {
                timeTextView.setText(currentTime);
                lastRenderedTime = currentTime;
            }

            String amPm = amPmFormat.format(now);
            if (!amPm.equals(lastRenderedAmPm)) {
                amPmTextView.setText(amPm);
                lastRenderedAmPm = amPm;
            }
            if (amPmTextView.getVisibility() != View.VISIBLE) {
                amPmTextView.setVisibility(View.VISIBLE);
            }
        }

        String currentDate = dateFormat.format(now).concat(batteryText);
        if (!currentDate.equals(lastRenderedDate)) {
            dateTextView.setText(currentDate);
            lastRenderedDate = currentDate;
        }
    }

    private void updateBatteryStatus(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level * 100 / (float) scale;
        String newBatteryText = String.format(Locale.getDefault(), " %.0f%%", batteryPct);
        if (!newBatteryText.equals(batteryText)) {
            batteryText = newBatteryText;
            updateTime();
        }
    }

    private void loadApps() {
        allApps = appManagementService.listApps();
        appLookup.clear();
        for (AppInfo app : allApps) {
            appLookup.put(buildAppKey(app.getPackageName(), app.getComponentName(), app.getUserId()), app);
        }
        selectedItems = settingsManager.getSelectedItems();
    }

    private void displaySelectedApps() {
        appLinearLayout.removeAllViews();
        for (SettingsManager.SelectedItem item : selectedItems) {
            if (item.getType().equals("application")) {
                Map<String, String> meta = item.getMeta();
                String packageName = meta.get("packageName");
                String componentName = meta.get("componentName");
                String userId = normalizeUserId(meta.get("userId"));
                AppInfo app = appLookup.get(buildAppKey(packageName, componentName, userId));
                if (app != null) {
                    addAppToLayout(app, item);
                }
            } else if (item.getType().equals("shortcut")) {
                addShortcutToLayout(item);
            }
        }

        if (settingsManager.isShowSettingsIcon()) {
            addSettingsAppToLayout();
        }
    }

    private void addAppToLayout(final AppInfo app, final SettingsManager.SelectedItem selectedItem) {
        View appView = getLayoutInflater().inflate(R.layout.app_item, null);
        ImageView iconView = appView.findViewById(R.id.appIcon);
        TextView nameView = appView.findViewById(R.id.appName);

        iconView.setImageDrawable(appManagementService.getIcon(app.getPackageName(), app.getUserId()));
        nameView.setText(app.getLabel());
        appView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appManagementService.launchApp(app);
            }
        });
        appView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showRemoveDialog(selectedItem, app.getLabel());
                return true;
            }
        });
        appLinearLayout.addView(appView);
    }

    private void addShortcutToLayout(final SettingsManager.SelectedItem shortcutItem) {
        Map<String, String> meta = shortcutItem.getMeta();
        final String name = meta.get("name");
        final String intentUri = meta.get("intent");
        String iconPath = meta.get("iconPath");

        if (name == null || intentUri == null) {
            return;
        }

        View appView = getLayoutInflater().inflate(R.layout.app_item, null);
        ImageView iconView = appView.findViewById(R.id.appIcon);
        TextView nameView = appView.findViewById(R.id.appName);

        nameView.setText(name);
        if (iconPath != null) {
            File iconFile = new File(iconPath);
            if (iconFile.exists()) {
                iconView.setImageBitmap(BitmapFactory.decodeFile(iconPath));
            }
        }

        appView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = Intent.parseUri(intentUri, 0);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (URISyntaxException e) {
                    Log.e(TAG, "Invalid shortcut intent URI: " + intentUri, e);
                }
            }
        });
        appView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showRemoveDialog(shortcutItem, name);
                return true;
            }
        });

        appLinearLayout.addView(appView);
    }

    private void showRemoveDialog(final SettingsManager.SelectedItem selectedItem, String itemName) {
        String message = getString(R.string.remove_from_main_screen, itemName);
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    settingsManager.deleteSelectedItem(selectedItem);
                    loadApps();
                    displaySelectedApps();
                })
                .show();
    }

    private void addSettingsAppToLayout() {
        View settingsAppView = getLayoutInflater().inflate(R.layout.app_item, null);
        ImageView iconView = settingsAppView.findViewById(R.id.appIcon);
        TextView nameView = settingsAppView.findViewById(R.id.appName);

        iconView.setImageResource(R.drawable.ic_settings);
        nameView.setText(R.string.launcher_settings);
        settingsAppView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettingsActivity();
            }
        });
        appLinearLayout.addView(settingsAppView);
    }

    private void openSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void openSearchActivity() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
    }

    private void initTimeFormatters() {
        time24Format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        time12Format = new SimpleDateFormat("hh:mm", Locale.getDefault());
        amPmFormat = new SimpleDateFormat("a", Locale.ENGLISH);
        dateFormat = new SimpleDateFormat(settingsManager.getDateFormat(), Locale.ENGLISH);
        lastRenderedTime = "";
        lastRenderedDate = "";
        lastRenderedAmPm = "";
    }

    private static String normalizeUserId(String userId) {
        if (userId == null || "null".equals(userId) || userId.isEmpty()) {
            return null;
        }
        return userId;
    }

    private static String buildAppKey(String packageName, String componentName, String userId) {
        return (packageName == null ? "" : packageName)
                + "|"
                + (componentName == null ? "" : componentName)
                + "|"
                + (userId == null ? "" : userId);
    }
}
