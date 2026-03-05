package com.zellopttt;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

public class PTTService extends AccessibilityService {

    private static final String TAG = "ZelloPTT";
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
                    new Handler(Looper.getMainLooper()).postDelayed(this::clickPTTButton, 1000);
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
                    return false;
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

    private void clickPTTButton() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Log.d(TAG, "Root is null");
            return;
        }
        
        // Log all clickable nodes so we can find the PTT button
        findAndClickPTT(root);
        root.recycle();
    }

    private void findAndClickPTT(AccessibilityNodeInfo node) {
        if (node == null) return;
        
        // Try to find by content description or class
        String desc = node.getContentDescription() != null ? 
                      node.getContentDescription().toString().toLowerCase() : "";
        String className = node.getClassName() != null ? 
                          node.getClassName().toString().toLowerCase() : "";
        
        Log.d(TAG, "Node: desc=" + desc + " class=" + className + " clickable=" + node.isClickable());
        
        if (node.isClickable() && (desc.contains("ptt") || desc.contains("talk") || 
            desc.contains("push") || desc.contains("transmit"))) {
            Log.d(TAG, "Found PTT button: " + desc);
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return;
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            findAndClickPTT(child);
            if (child != null) child.recycle();
        }
    }
}
```

Commit, build, uninstall, install. Then after pressing the button run this to see what Zello's PTT button is actually called:
```
C:\Users\dovyg\Downloads\platform-tools-latest-windows\platform-tools\adb.exe shell logcat -s ZelloPTT
