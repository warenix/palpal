package org.dyndns.warenix.palpal;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by warenix on 11/8/15.
 */
public class MyGcmListenerService extends GcmListenerService {
    private static final String TAG = "MyGcmListenerService";

    private static AtomicInteger sNotificationId = new AtomicInteger();

    private OkHttpClient mClient;


    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        for (String key : data.keySet()) {
            Log.d(TAG, "key:" + key);
        }
        String message = data.getString("data");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        sendNotification(message);
        // [END_EXCLUDE]
    }
    // [END receive_message]


    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        String name = null;
        String screenName = null;
        String text = null;
        String idString = null;
        String profileImageUrl = null;
        try {
            JSONObject jsonObject = new JSONObject(message);
            name = jsonObject.getString("name");
            screenName = jsonObject.getString("screen_name");
            text = jsonObject.getString("text");
            idString = jsonObject.getString("id_str");
            profileImageUrl = jsonObject.getString("profile_image_url");

        } catch (JSONException e) {
            e.printStackTrace();
        }


        Bitmap bitmap = null;
        if (profileImageUrl != null) {
//            OkHttpDownloader downloader = new OkHttpDownloader(getApplicationContext());
            try {
                profileImageUrl = profileImageUrl.replace("_normal", "_bigger");
                Request request = new Request.Builder()
                        .url(profileImageUrl)
                        .build();
                if (mClient == null) {
                    File cacheDirectory = getCacheDir();
                    int cacheSize = 50 * 1024 * 1024; // 10 MiB
                    Cache cache = new Cache(cacheDirectory, cacheSize);

                    mClient = new OkHttpClient();
                    mClient.setCache(cache);
                }
                Response response = mClient.newCall(request).execute();
//                Downloader.Response response = downloader.load(Uri.parse(profileImageUrl), 0);
                bitmap = BitmapFactory.decodeStream(response.body().byteStream());

                Resources res = getResources();
                int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
                int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(String.format("https://twitter.com/%s/status/%s", screenName, idString)));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_textsms_24dp)
                .setTicker(getResources().getString(R.string.app_name)).setWhen(0)
                .setContentTitle(String.format("%s @%s", name, screenName))
                .setContentText(text)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setLargeIcon(bitmap)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(sNotificationId.incrementAndGet() /* ID of notification */, notificationBuilder.build());
    }
}
