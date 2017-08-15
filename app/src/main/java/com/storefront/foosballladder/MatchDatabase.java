package com.storefront.foosballladder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MatchDatabase extends SQLiteOpenHelper {
    MatchDatabase(Context context) {
        super(context, "MatchDatabase", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(DatabaseContract.Team.SQL_CREATE_TABLE);
        sqLiteDatabase.execSQL(DatabaseContract.MatchResult.SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
