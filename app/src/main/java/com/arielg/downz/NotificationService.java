package com.arielg.downz;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.util.Log;

public class NotificationService extends IntentService {

    private static final String TAG = "<<<      Notify     >>>";

    private final static String ACTION_CANCEL_DOWNLOAD = "com.arielg.downz.action.CANCEL_DOWNLOAD";

    private static final int NOTIFICATION_ID = 242;

    private final static NotificationService mInstance = new NotificationService();

    private static Bitmap mLargeIcon = null;

    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("WeakerAccess")
    public NotificationService() {
        super("NotificationService");
    }

    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {

            final String action = intent.getAction();
            Log.d(TAG, "action: " + action);

            if (ACTION_CANCEL_DOWNLOAD.equals(action)) {
                MainActivity.cancelDownload();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    public static void Notify(Context context, String text, boolean addCancelAction, int progressTotal, int progressInc) {
        mInstance.notify(context, text, addCancelAction, progressTotal, progressInc);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    public static void Cancel(Context context) {
        NotificationManager notifyMgr = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
        notifyMgr.cancel(NOTIFICATION_ID);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    private void notify(Context context, String text, boolean addCancelAction, int progressTotal, int progressInc) {

        if(mLargeIcon == null) {
            Drawable drawable = context.getResources().getDrawable(R.mipmap.ic_launcher, null);
            mLargeIcon = ( drawable != null ? ((BitmapDrawable)drawable).getBitmap() : null );
        }

        Notification.Style style = null;

        if(text.contains("\n")) {
            style = new Notification.BigTextStyle().bigText(text);
            /*  style = new Notification.InboxStyle();
                String[] lines = text.split("[\\r\\n]");
                for (String s : lines) {
                    ((Notification.InboxStyle)style).addLine(s);
                } */
        }

        Notification.Builder builder = new Notification.Builder(context)
                .setTicker(context.getResources().getString(R.string.app_name))
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_action_download_24dp)
                .setLargeIcon(mLargeIcon)
                .setStyle(style)
                .setProgress(progressTotal, progressInc, false);

        if(addCancelAction) {
            Intent dismissIntent = new Intent(context, NotificationService.class);
            dismissIntent.setAction(ACTION_CANCEL_DOWNLOAD);
            PendingIntent piCancel = PendingIntent.getService(context, NOTIFICATION_ID, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Icon ic = Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel);
            Notification.Action actionCancel = new Notification.Action.Builder(ic, "Cancel", piCancel).build();

            builder.addAction(actionCancel);
        }

        // Gets an instance of the NotificationManager service
        NotificationManager notifyMgr = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);

        notifyMgr.notify(NOTIFICATION_ID, builder.build());
    }
}
