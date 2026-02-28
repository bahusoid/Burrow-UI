package com.hamsterbase.burrowui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles Android 8.0+ (API 26) shortcut pinning requests via
 * LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT. Accepts the shortcut automatically
 * and stores it so MainActivity can display it.
 */
@TargetApi(Build.VERSION_CODES.O)
public class ConfirmPinShortcutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            finish();
            return;
        }

        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        if (launcherApps == null) {
            finish();
            return;
        }

        LauncherApps.PinItemRequest request = launcherApps.getPinItemRequest(getIntent());
        if (request == null || !request.isValid()) {
            finish();
            return;
        }

        if (request.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            ShortcutInfo shortcutInfo = request.getShortcutInfo();
            if (shortcutInfo != null) {
                storeShortcut(shortcutInfo);
                request.accept();
            }
        }

        finish();
    }

    private void storeShortcut(ShortcutInfo shortcutInfo) {
        String name = shortcutInfo.getShortLabel() != null
                ? shortcutInfo.getShortLabel().toString()
                : shortcutInfo.getId();

        String intentUri = null;
        if (shortcutInfo.getIntent() != null) {
            intentUri = shortcutInfo.getIntent().toUri(0);
        }
        if (intentUri == null) {
            return;
        }

        String iconPath = null;
        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        if (launcherApps != null) {
            Drawable iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0);
            if (iconDrawable != null) {
                iconPath = saveDrawableToFile(iconDrawable);
            }
        }

        Map<String, String> meta = new HashMap<>();
        meta.put("name", name);
        meta.put("intent", intentUri);
        if (iconPath != null) {
            meta.put("iconPath", iconPath);
        }

        SettingsManager settingsManager = new SettingsManager(this);
        settingsManager.pushSelectedItem(new SettingsManager.SelectedItem("shortcut", meta));
    }

    private String saveDrawableToFile(Drawable drawable) {
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

        File iconsDir = new File(getFilesDir(), "shortcut_icons");
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
}
