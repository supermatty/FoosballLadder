package com.storefront.foosballladder;

import android.provider.BaseColumns;

public class DatabaseContract {
    private DatabaseContract() {}

    public static class Team implements BaseColumns {
        public static final String TABLE_NAME = "teams";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_ACTIVE_MONTH = "active_month";
        public static final String COLUMN_NAME_ACTIVE_YEAR = "active_year";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAME_NAME + " TEXT, " +
                        COLUMN_NAME_ACTIVE_MONTH + " INTEGER, "+
                        COLUMN_NAME_ACTIVE_YEAR + " INTEGER)";
    }

    public static class MatchResult implements BaseColumns {
        public static final String TABLE_NAME = "match_result";
        public static final String COLUMN_NAME_WINNER_ID = "winner_id";
        public static final String COLUMN_NAME_LOSER_ID = "loser_id";
        public static final String COLUMN_NAME_LOSER_SCORE = "loser_score";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAME_WINNER_ID + " INTEGER, " +
                        COLUMN_NAME_LOSER_ID + " INTEGER, " +
                        COLUMN_NAME_LOSER_SCORE + " INTEGER)";
    }
}
