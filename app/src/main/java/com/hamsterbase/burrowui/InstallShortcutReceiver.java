package com.hamsterbase.burrowui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Receives com.android.launcher.action.INSTALL_SHORTCUT broadcasts (pre-API 26 shortcut pinning).
 * Stores the shortcut name, launch intent and icon so MainActivity can display it.
 */
public class InstallShortcutReceiver extends BroadcastReceiver {

    static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_INSTALL_SHORTCUT.equals(intent.getAction())) {
            return;
        }

        String name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Intent shortcutIntent = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        if (name == null || shortcutIntent == null) {
            return;
        }

        String iconPath = null;

        // Try to get an explicit Bitmap icon first
        Bitmap iconBitmap = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (iconBitmap != null) {
            iconPath = saveBitmapToFile(context, iconBitmap);
        }

        // Fall back to a resource-based icon
        if (iconPath == null) {
            Intent.ShortcutIconResource iconResource =
                    intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (iconResource != null) {
                iconPath = saveIconResourceToFile(context, iconResource);
            }
        }

        Map<String, String> meta = new HashMap<>();
        meta.put("name", name);
        meta.put("intent", shortcutIntent.toUri(0));
        if (iconPath != null) {
            meta.put("iconPath", iconPath);
        }

        SettingsManager settingsManager = new SettingsManager(context);
        settingsManager.pushSelectedItem(new SettingsManager.SelectedItem("shortcut", meta));
    }

    private String saveBitmapToFile(Context context, Bitmap bitmap) {
        File iconsDir = new File(context.getFilesDir(), "shortcut_icons");
        if (!iconsDir.exists() && !iconsDir.mkdirs()) {
            return null;
        }
        File iconFile = new File(iconsDir, UUID.randomUUID() + ".png");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(iconFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            return iconFile.getAbsolutePath();
        } catch (IOException e) {
            return null;
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (IOException ignored) {}
            }
        }
    }

    // Resources.getDrawable(int) is deprecated since API 22, but we support API 19+
    // where the two-argument overload Resources.getDrawable(int, Theme) is not available.
    @SuppressWarnings("deprecation")
    private String saveIconResourceToFile(Context context, Intent.ShortcutIconResource iconResource) {
        try {
            PackageManager pm = context.getPackageManager();
            Resources resources = pm.getResourcesForApplication(iconResource.packageName);
            int resId = resources.getIdentifier(iconResource.resourceName, null, null);
            if (resId == 0) {
                return null;
            }
            Drawable drawable = resources.getDrawable(resId);
            if (drawable == null) {
                return null;
            }
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                int w = drawable.getIntrinsicWidth();
                int h = drawable.getIntrinsicHeight();
                if (w <= 0) w = 48;
                if (h <= 0) h = 48;
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
            return saveBitmapToFile(context, bitmap);
        } catch (Exception e) {
            return null;
        }
    }
}
