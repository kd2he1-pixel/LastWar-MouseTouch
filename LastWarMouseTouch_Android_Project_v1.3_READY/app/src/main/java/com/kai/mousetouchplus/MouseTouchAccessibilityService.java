package com.kai.mousetouchplus;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public class MouseTouchAccessibilityService extends AccessibilityService {
    private static final String LAST_WAR = "com.fun.lastwar.gp";
    private boolean lastWarActive = false;
    private WindowManager wm;
    private CursorView cursor;
    private WindowManager.LayoutParams cursorLp;
    private float lastX, lastY;
    private long lastTapAt;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        setMouseCapture(false);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence p = event.getPackageName();
        if (p == null) return;
        boolean active = LAST_WAR.contentEquals(p);
        if (active != lastWarActive) {
            lastWarActive = active;
            setMouseCapture(active);
            if (!active) hideCursor();
        }
    }

    private void setMouseCapture(boolean enabled) {
        if (Build.VERSION.SDK_INT < 34) return;
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) return;
        info.setMotionEventSources(enabled ? InputDevice.SOURCE_MOUSE : 0);
        setServiceInfo(info);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (Build.VERSION.SDK_INT < 34 || !lastWarActive) return;
        if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) return;

        lastX = event.getRawX();
        lastY = event.getRawY();
        showCursor(lastX, lastY);

        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_BUTTON_RELEASE) {
            if ((event.getActionButton() & MotionEvent.BUTTON_PRIMARY) != 0) {
                tap(lastX, lastY);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            if (System.currentTimeMillis() - lastTapAt > 180) tap(lastX, lastY);
        }
    }

    private void tap(float x, float y) {
        lastTapAt = System.currentTimeMillis();
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 80);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke).build();
        dispatchGesture(gesture, null, null);
        if (cursor != null) cursor.flash();
    }

    private void showCursor(float x, float y) {
        if (wm == null) return;
        if (cursor == null) {
            cursor = new CursorView();
            int size = dp(44);
            cursorLp = new WindowManager.LayoutParams(
                    size, size,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.graphics.PixelFormat.TRANSLUCENT);
            cursorLp.gravity = Gravity.TOP | Gravity.START;
            cursorLp.x = (int) x;
            cursorLp.y = (int) y;
            try { wm.addView(cursor, cursorLp); } catch (Exception ignored) { }
        } else {
            cursorLp.x = (int) x;
            cursorLp.y = (int) y;
            try { wm.updateViewLayout(cursor, cursorLp); } catch (Exception ignored) { }
        }
    }

    private void hideCursor() {
        if (wm != null && cursor != null) {
            try { wm.removeView(cursor); } catch (Exception ignored) { }
        }
        cursor = null;
        cursorLp = null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        setMouseCapture(false);
        hideCursor();
        super.onDestroy();
    }

    private class CursorView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private long flashUntil;

        CursorView() {
            super(MouseTouchAccessibilityService.this);
            fill.setColor(Color.WHITE);
            fill.setStyle(Paint.Style.FILL);
            stroke.setColor(Color.rgb(25, 35, 55));
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(dp(2));
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void flash() {
            flashUntil = System.currentTimeMillis() + 120;
            invalidate();
            postDelayed(this::invalidate, 130);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            float s = getWidth();
            Path p = new Path();
            p.moveTo(2, 2);
            p.lineTo(s * 0.64f, s * 0.47f);
            p.lineTo(s * 0.40f, s * 0.53f);
            p.lineTo(s * 0.56f, s * 0.86f);
            p.lineTo(s * 0.43f, s * 0.92f);
            p.lineTo(s * 0.28f, s * 0.60f);
            p.lineTo(s * 0.12f, s * 0.79f);
            p.close();
            c.drawPath(p, fill);
            c.drawPath(p, stroke);
            if (System.currentTimeMillis() < flashUntil) {
                Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
                ring.setStyle(Paint.Style.STROKE);
                ring.setStrokeWidth(dp(2));
                ring.setColor(Color.rgb(0, 180, 255));
                c.drawCircle(2, 2, dp(12), ring);
            }
        }
    }
}
