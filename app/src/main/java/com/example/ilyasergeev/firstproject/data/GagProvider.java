package com.example.ilyasergeev.firstproject.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class GagProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private GagDbHelper mOpenHelper;

    static final int FRESH_GAG = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int TYPE = 300;

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static{
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationSettingQueryBuilder.setTables(
                GagContract.GagEntry.TABLE_NAME + " INNER JOIN " +
                        GagContract.TypeEntry.TABLE_NAME +
                        " ON " + GagContract.GagEntry.TABLE_NAME +
                        "." + GagContract.GagEntry.COLUMN_TYPE_ID +
                        " = " + GagContract.TypeEntry.TABLE_NAME +
                        "." + GagContract.TypeEntry._ID);
    }

    private static final String sTypeSettingSelection =
            GagContract.TypeEntry.TABLE_NAME+
                    "." + GagContract.TypeEntry.COLUMN_TYPE_SETTING + " = ? ";

    private static final String sTypeSettingWithStartDateSelection =
            GagContract.TypeEntry.TABLE_NAME+
                    "." + GagContract.TypeEntry.COLUMN_TYPE_SETTING + " = ? AND " +
                    GagContract.GagEntry.COLUMN_DATE + " >= ? ";

    private static final String sTypeSettingAndDaySelection =
            GagContract.TypeEntry.TABLE_NAME +
                    "." + GagContract.TypeEntry.COLUMN_TYPE_SETTING + " = ? AND " +
                    GagContract.GagEntry.COLUMN_DATE + " = ? ";

    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = GagContract.GagEntry.getTypeSettingFromUri(uri);
        long startDate = GagContract.GagEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = sTypeSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selectionArgs = new String[]{locationSetting, Long.toString(startDate)};
            selection = sTypeSettingWithStartDateSelection;
        }

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getWeatherByLocationSettingAndDate(
            Uri uri, String[] projection, String sortOrder) {
        String locationSetting = GagContract.GagEntry.getTypeSettingFromUri(uri);
        long date = GagContract.GagEntry.getDateFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sTypeSettingAndDaySelection,
                new String[]{locationSetting, Long.toString(date)},
                null,
                null,
                sortOrder
        );
    }

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = GagContract.CONTENT_AUTHORITY;
        matcher.addURI(authority, GagContract.PATH_GAG, FRESH_GAG);
        matcher.addURI(authority, GagContract.PATH_GAG + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, GagContract.PATH_GAG + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority, GagContract.PATH_TYPE, TYPE);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new GagDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case WEATHER_WITH_LOCATION_AND_DATE:
                return GagContract.GagEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return GagContract.GagEntry.CONTENT_TYPE;
            case FRESH_GAG:
                return GagContract.GagEntry.CONTENT_TYPE;
            case TYPE:
                return GagContract.TypeEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case WEATHER_WITH_LOCATION_AND_DATE:
            {
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            }
            case WEATHER_WITH_LOCATION: {
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            }
            case FRESH_GAG: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        GagContract.GagEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case TYPE: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        GagContract.TypeEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case FRESH_GAG: {
                normalizeDate(values);
                long _id = db.insert(GagContract.GagEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = GagContract.GagEntry.buildWeatherUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case TYPE: {
                long _id = db.insert(GagContract.TypeEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = GagContract.TypeEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case FRESH_GAG:
                rowsDeleted = db.delete(
                        GagContract.GagEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case TYPE:
                rowsDeleted = db.delete(
                        GagContract.TypeEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(GagContract.GagEntry.COLUMN_DATE)) {
            //long dateValue = values.getAsLong(GagContract.GagEntry.COLUMN_DATE);
            //values.put(GagContract.GagEntry.COLUMN_DATE, GagContract.normalizeDate(dateValue));
        }
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case FRESH_GAG:
                normalizeDate(values);
                rowsUpdated = db.update(GagContract.GagEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case TYPE:
                rowsUpdated = db.update(GagContract.TypeEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case FRESH_GAG:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        long _id = db.insert(GagContract.GagEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}