package com.hamsterbase.burrowui;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.hamsterbase.burrowui.service.AppInfo;
import com.hamsterbase.burrowui.service.AppManagementService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MainScreenItemMapper {

    private MainScreenItemMapper() {
    }

    public static List<MainScreenItem> map(List<SettingsManager.SelectedItem> selectedItems,
                                           List<AppInfo> allApps,
                                           AppManagementService appManagementService,
                                           android.content.Context context) {
        Map<String, AppInfo> appLookup = new HashMap<>();
        for (AppInfo app : allApps) {
            appLookup.put(buildAppKey(app.getPackageName(), app.getComponentName(), app.getUserId()), app);
        }

        List<MainScreenItem> result = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            SettingsManager.SelectedItem selectedItem = selectedItems.get(i);
            if ("application".equals(selectedItem.getType())) {
                MainScreenItem appItem = mapApplicationItem(selectedItem, i, appLookup, appManagementService);
                if (appItem != null) {
                    result.add(appItem);
                }
            } else if ("shortcut".equals(selectedItem.getType())) {
                MainScreenItem shortcutItem = mapShortcutItem(selectedItem, i, context);
                if (shortcutItem != null) {
                    result.add(shortcutItem);
                }
            }
        }
        return result;
    }

    private static MainScreenItem mapApplicationItem(SettingsManager.SelectedItem selectedItem,
                                                     int originalIndex,
                                                     Map<String, AppInfo> appLookup,
                                                     AppManagementService appManagementService) {
        Map<String, String> meta = selectedItem.getMeta();
        String packageName = meta.get("packageName");
        String componentName = meta.get("componentName");
        String userId = normalizeUserId(meta.get("userId"));

        AppInfo app = appLookup.get(buildAppKey(packageName, componentName, userId));
        if (app == null) {
            return null;
        }

        Drawable icon = appManagementService.getIcon(app.getPackageName(), app.getUserId());
        return MainScreenItem.forApplication(selectedItem, originalIndex, app.getLabel(), icon, app);
    }

    private static MainScreenItem mapShortcutItem(SettingsManager.SelectedItem selectedItem,
                                                  int originalIndex,
                                                  android.content.Context context) {
        Map<String, String> meta = selectedItem.getMeta();
        String name = meta.get("name");
        String intentUri = meta.get("intent");
        String iconPath = meta.get("iconPath");

        if (name == null || intentUri == null) {
            return null;
        }

        Drawable icon = null;
        if (iconPath != null) {
            File iconFile = new File(iconPath);
            if (iconFile.exists()) {
                icon = new BitmapDrawable(context.getResources(), BitmapFactory.decodeFile(iconPath));
            }
        }

        return MainScreenItem.forShortcut(selectedItem, originalIndex, name, icon, intentUri);
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

    public static class MainScreenItem {
        private final SettingsManager.SelectedItem selectedItem;
        private final int originalIndex;
        private final String label;
        private final Drawable icon;
        private final AppInfo app;
        private final String shortcutIntentUri;

        private MainScreenItem(SettingsManager.SelectedItem selectedItem,
                               int originalIndex,
                               String label,
                               Drawable icon,
                               AppInfo app,
                               String shortcutIntentUri) {
            this.selectedItem = selectedItem;
            this.originalIndex = originalIndex;
            this.label = label;
            this.icon = icon;
            this.app = app;
            this.shortcutIntentUri = shortcutIntentUri;
        }

        static MainScreenItem forApplication(SettingsManager.SelectedItem selectedItem,
                                             int originalIndex,
                                             String label,
                                             Drawable icon,
                                             AppInfo app) {
            return new MainScreenItem(selectedItem, originalIndex, label, icon, app, null);
        }

        static MainScreenItem forShortcut(SettingsManager.SelectedItem selectedItem,
                                          int originalIndex,
                                          String label,
                                          Drawable icon,
                                          String shortcutIntentUri) {
            return new MainScreenItem(selectedItem, originalIndex, label, icon, null, shortcutIntentUri);
        }

        public SettingsManager.SelectedItem getSelectedItem() {
            return selectedItem;
        }

        public int getOriginalIndex() {
            return originalIndex;
        }

        public String getLabel() {
            return label;
        }

        public Drawable getIcon() {
            return icon;
        }

        public AppInfo getApp() {
            return app;
        }

        public String getShortcutIntentUri() {
            return shortcutIntentUri;
        }

        public boolean isApplication() {
            return app != null;
        }

        public boolean isShortcut() {
            return shortcutIntentUri != null;
        }
    }
}
