package com.jakevin.bluenotify;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

/**
 * Created by jakevin on 2014/7/17.
 */
public class ConnectService extends AccessibilityService {

    private static final String TAG = "NotificationFetcherService: ";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!(event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) ){
            return;
        }

        Notification localNotification = (Notification)event.getParcelableData();

        if (localNotification != null) {
            Intent intent=new Intent();
            intent.putExtra("NotifyData_Title", localNotification.tickerText);

//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            drawableToBitmap(extractImage(localNotification.contentView)).compress(Bitmap.CompressFormat.PNG, 100, stream);
//            byte[] byteArray = stream.toByteArray();

            final PackageManager pm = getApplicationContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo( event.getPackageName().toString(), 0);
            } catch (final PackageManager.NameNotFoundException e) {
                ai = null;
            }
            final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

            intent.putExtra("NotifyData_CT", applicationName);

            intent.setAction(".ConnectService");
            Log.d("DX:",""+event.getPackageName());
            sendBroadcast(intent);
        }
    }

    @Override
    protected void onServiceConnected() {

        // Define it in both xml file and here,  for compatibility with pre-ICS devices
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
    }

    @Override
    public void onInterrupt() {
        System.out.println("onInterrupt");
    }

    private Drawable extractImage(RemoteViews views) {
        LinearLayout ll = new LinearLayout(getApplicationContext());
        View view = views.apply(getApplicationContext(), ll);
        return  searchForBitmap(view);
       // Log.d("Drawable: " + drawable);
    }

    private Drawable searchForBitmap(View view) {
        if (view instanceof ImageView) {
            return ((ImageView) view).getDrawable();
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                Drawable result = searchForBitmap(viewGroup.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
