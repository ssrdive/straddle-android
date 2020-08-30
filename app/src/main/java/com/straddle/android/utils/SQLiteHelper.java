package com.straddle.android.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class SQLiteHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "straddle.db";

    public SQLiteHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE received_message (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "message_id INTEGER NOT NULL," +
                "from_user TEXT NOT NULL," +
                "message TEXT NOT NULL," +
                "timestamp TEXT NOT NULL," +
                "read_timestamp TEXT)");
        db.execSQL("CREATE TABLE sent_message (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "sent INTEGER NOT NULL DEFAULT 0," +
                "sent_timestamp TEXT," +
                "to_user TEXT NOT NULL," +
                "message TEXT NOT NULL," +
                "timestamp TEXT NOT NULL," +
                "read_timestamp TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(newVersion > oldVersion) {
            db.execSQL("DROP TABLE IF EXISTS received_message");
            db.execSQL("DROP TABLE IF EXISTS sent_message");
            onCreate(db);
        }
    }
}
