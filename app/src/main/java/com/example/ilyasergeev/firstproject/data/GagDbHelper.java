package com.example.ilyasergeev.firstproject.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.ilyasergeev.firstproject.data.GagContract.GagEntry;
import com.example.ilyasergeev.firstproject.data.GagContract.TypeEntry;

public class GagDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;

    static final String DATABASE_NAME = "gag.db";

    public GagDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_LOCATION_TABLE = "CREATE TABLE " + TypeEntry.TABLE_NAME + " (" +
                TypeEntry._ID + " INTEGER PRIMARY KEY," +
                TypeEntry.COLUMN_TYPE_SETTING + " TEXT UNIQUE NOT NULL " +
                " );";

        final String SQL_CREATE_WEATHER_TABLE = "CREATE TABLE " + GagEntry.TABLE_NAME + " (" +

                GagContract.GagEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                GagContract.GagEntry.COLUMN_TYPE_ID + " INTEGER NOT NULL, " +
                GagEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                GagEntry.COLUMN_GAG_IMAGE + " BLOB NOT NULL, " +
                GagEntry.COLUMN_GAG_ID + " INTEGER NOT NULL," +
                GagEntry.COLUMN_CAPTION + " TEXT NOT NULL, " +

                " FOREIGN KEY (" + GagEntry.COLUMN_TYPE_ID + ") REFERENCES " +
                TypeEntry.TABLE_NAME + " (" + TypeEntry._ID + "), " +

                " UNIQUE (" + GagEntry.COLUMN_DATE + ", " +
                GagContract.GagEntry.COLUMN_TYPE_ID + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_LOCATION_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_WEATHER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TypeEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + GagEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
