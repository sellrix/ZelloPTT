package com.zellopttt;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public class PTTService extends AccessibilityService {

    private static final int KEYCODE_ZELLO_PTT = 250;
    private static final String ZELLO_PACKAGE = "com.loudtalks";
    private String currentForegroundPackage = "";
    private boolean zelloJustOpened = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null) {
                String pkg = event.getPackageName().toString();
                if (ZELLO_PACKAGE.equals(pkg) && zelloJustOpened) {
                    zelloJustOpened = false;
                    new Handler(Looper.getMainLooper()).postDelayed(this::tapZelloPTT, 800);
                }
                currentForegroundPackage = pkg;
            }
        }
    }

    @Override
    public void onInterrupt() {}

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getScanCode() == KEYCODE_ZELLO_PTT || event.getKeyCode() == KEYCODE_ZELLO_PTT) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (ZELLO_PACKAGE.equals(currentForegroundPackage)) {
                    return false; // Zello is open, let it handle natively
                } else {
                    zelloJustOpened = true;
                    launchZello();
                    return true;
                }
            }
        }
        return false;
    }

    private void launchZello() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(ZELLO_PACKAGE);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                          Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(launch);
        }
    }

    private void tapZelloPTT() {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE))
                .getDefaultDisplay().getRealMetrics(metrics);

        float x = metrics.widthPixels / 2f;
        float y = metrics.heightPixels * 0.82f; // PTT button is near bottom

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 500))
                .build();

        dispatchGesture(gesture, null, null);
    }
}
