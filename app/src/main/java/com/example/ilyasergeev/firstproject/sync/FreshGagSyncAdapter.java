package com.example.ilyasergeev.firstproject.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.example.ilyasergeev.firstproject.MainActivity;
import com.example.ilyasergeev.firstproject.R;
import com.example.ilyasergeev.firstproject.Utility;
import com.example.ilyasergeev.firstproject.data.GagContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

public class FreshGagSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = FreshGagSyncAdapter.class.getSimpleName();
    public static final int SYNC_INTERVAL = 60*10;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    private static final long DAY_IN_MILLIS = 60;
    private static final int GAG_NOTIFICATION_ID = 3004;


    private static final String[] NOTIFY_GAG_PROJECTION = new String[] {
            GagContract.GagEntry.COLUMN_GAG_ID,
            GagContract.GagEntry.COLUMN_CAPTION,
            GagContract.GagEntry.COLUMN_GAG_IMAGE
    };

    // these indices must match the projection
    private static final int INDEX_GAG_ID = 0;
    private static final int INDEX_CAPTION = 1;

    public FreshGagSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");
        String typeQuery = Utility.getPreferredLocation(getContext());
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        HttpURLConnection urlConnection2 = null;
        BufferedReader reader2 = null;
        String gagJsonStr = null;

        try {

            final String GAG_BASE_URL="http://infinigag.eu01.aws.af.cm/"+typeQuery+"/0";
            Uri builtUri2 = Uri.parse(GAG_BASE_URL).buildUpon().build();
            URL url2 = new URL(builtUri2.toString());
            urlConnection2 = (HttpURLConnection) url2.openConnection();
            urlConnection2.setRequestMethod("GET");
            urlConnection2.connect();
            InputStream inputStream2 = urlConnection2.getInputStream();
            StringBuffer buffer2 = new StringBuffer();
            if (inputStream2 == null) {
                return;
            }
            reader2 = new BufferedReader(new InputStreamReader(inputStream2));
            String line2;
            while ((line2 = reader2.readLine()) != null) {
                buffer2.append(line2 + "\n");
            }
            if (buffer2.length() == 0) {
                return;
            }
            gagJsonStr = buffer2.toString();

            getGagDataFromJson(gagJsonStr, typeQuery);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return;
    }

    private void getGagDataFromJson(String gagJsonStr,
                                    String locationSetting)
            throws JSONException {

        final String CAPTION = "caption";
        try {

            JSONObject gagJson = new JSONObject(gagJsonStr);
            JSONArray gagArray = gagJson.getJSONArray("data");

            long typeId = addType(locationSetting);
            Vector<ContentValues> cVVector = new Vector<ContentValues>(gagArray.length());

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new Time();

            for(int i = 0; i < gagArray.length(); i++) {
                long dateTime;
                String caption;
                String gagId;

                JSONObject gag = gagArray.getJSONObject(i);
                String imageUrl = gag.optJSONObject("images").optString("large");
                dateTime = dayTime.setJulianDay(julianStartDay+i);

                HttpURLConnection urlConnection = (HttpURLConnection)new URL(imageUrl).openConnection();
                InputStream inputStream = new URL(imageUrl).openConnection().getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                buffer.flush();
                byte[] gagImage = buffer.toByteArray();
                caption = gag.getString(CAPTION);
                gagId = gag.getString("id");
                ContentValues gagValues = new ContentValues();

                gagValues.put(GagContract.GagEntry.COLUMN_TYPE_ID, typeId);
                gagValues.put(GagContract.GagEntry.COLUMN_DATE, dateTime);
                gagValues.put(GagContract.GagEntry.COLUMN_CAPTION, caption);
                gagValues.put(GagContract.GagEntry.COLUMN_GAG_IMAGE, gagImage);
                gagValues.put(GagContract.GagEntry.COLUMN_GAG_ID, gagId);

                cVVector.add(gagValues);
            }

            int inserted = 0;
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(GagContract.GagEntry.CONTENT_URI, cvArray);

                getContext().getContentResolver().delete(GagContract.GagEntry.CONTENT_URI,
                        GagContract.GagEntry.COLUMN_DATE + " <= ?",
                        new String[] {Long.toString(dayTime.setJulianDay(julianStartDay-1))});

                notifyGag();
            }

            Log.d(LOG_TAG, "Sync Complete. " + cVVector.size() + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyGag() {
        Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if ( displayNotifications ) {

            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                String locationQuery = Utility.getPreferredLocation(context);

                Uri weatherUri = GagContract.GagEntry.buildGagWithDate(locationQuery, System.currentTimeMillis());

                Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_GAG_PROJECTION, null, null, null);

                if (cursor.moveToFirst()) {
                    //int gagId = cursor.getInt(INDEX_GAG_ID);
                    byte[] image = cursor.getBlob(2);
                    //int iconId = 801;
                    Resources resources = context.getResources();
                    Bitmap largeIcon = BitmapFactory.decodeByteArray(image, 0, image.length);
                    String title = context.getString(R.string.app_name);

                    String contentText = cursor.getString(INDEX_CAPTION);

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getContext())
                                    .setColor(resources.getColor(R.color.sunshine_light_blue))
                                    .setSmallIcon(R.drawable.ic_clear)
                                    .setLargeIcon(largeIcon)
                                    .setContentTitle(title)
                                    .setContentText(contentText);

                    Intent resultIntent = new Intent(context, MainActivity.class);

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager =
                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(GAG_NOTIFICATION_ID, mBuilder.build());

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();
                }
                cursor.close();
            }
        }
    }

    long addType(String locationSetting) {
        long typeId;

        Cursor typeCursor = getContext().getContentResolver().query(
                GagContract.TypeEntry.CONTENT_URI,
                new String[]{GagContract.TypeEntry._ID},
                GagContract.TypeEntry.COLUMN_TYPE_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if (typeCursor.moveToFirst()) {
            int locationIdIndex = typeCursor.getColumnIndex(GagContract.TypeEntry._ID);
            typeId = typeCursor.getLong(locationIdIndex);
        } else {
            ContentValues typeValues = new ContentValues();
            typeValues.put(GagContract.TypeEntry.COLUMN_TYPE_SETTING, locationSetting);

            Uri insertedUri = getContext().getContentResolver().insert(
                    GagContract.TypeEntry.CONTENT_URI,
                    typeValues
            );

            typeId = ContentUris.parseId(insertedUri);
        }

        typeCursor.close();
        return typeId;
    }

    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    public static Account getSyncAccount(Context context) {
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        if ( null == accountManager.getPassword(newAccount) ) {


            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        FreshGagSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}