package com.zellopttt;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class PTTService extends AccessibilityService {

    private static final int KEYCODE_ZELLO_PTT = 250;
    private static final String ZELLO_PACKAGE = "com.loudtalks";
    private String currentForegroundPackage = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null) {
                currentForegroundPackage = event.getPackageName().toString();
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
                    // Zello is open — let it handle the key natively
                    return false;
                } else {
                    // Zello is closed — open it
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
}
