package com.naman14.timber.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class RatingStore {

    private static final int DEFAULT = 5;
    private static final int MAX_ITEMS_IN_DB = 100;

    private static RatingStore sInstance = null;

    private MusicDB mMusicDatabase = null;

    private RatingStore(final Context context) {
        mMusicDatabase = MusicDB.getInstance(context);
    }


    /**
     * @param context The {@link android.content.Context} to use
     * @return A new instance of this class.
     */
    public static final synchronized RatingStore getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new RatingStore(context.getApplicationContext());
        }
        return sInstance;
    }

//    private RatingStore(Context applicationContext) {

    //  }


    public void onCreate(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + RatingStoreColumns.NAME + " ("
                + RatingStoreColumns.ID + " LONG NOT NULL PRIMARY KEY," + RatingStoreColumns.RATING
                + " LONG NOT NULL);");
    }

    public Cursor queryRatingIds(final String limit) {
        final SQLiteDatabase database = mMusicDatabase.getReadableDatabase();
        return database.query(RatingStoreColumns.NAME,
                new String[]{RatingStoreColumns.ID}, null, null, null, null,
                RatingStoreColumns.RATING + " DESC", null);
    }

    public void onUpgrate(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RatingStoreColumns.NAME);
        onCreate(db);
    }

    public int getRating(long songId) {
        final SQLiteDatabase database = mMusicDatabase.getReadableDatabase();
        Cursor ratings = database.query(RatingStoreColumns.NAME,
                new String[]{RatingStoreColumns.RATING}, RatingStoreColumns.ID + "=" + songId, null, null, null,
                null, "1");

        try {
            if (ratings.getCount() == 0) {
                return DEFAULT;
            }
            if (ratings.getCount() > 1) {
                throw new IllegalStateException("Unexpected count of ratings for the song " + songId);
            }
            return ratings.getInt(0);
        } finally {
            ratings.close();
        }
    }

    public void setRainting(int songId, int rating) {
        System.out.println("NEXT   " + songId + ' '  + rating + '\n' );
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();

        database.beginTransaction();
        try {
            ContentValues values = new ContentValues(2);
            values.put(RatingStoreColumns.ID, songId);
            values.put(RatingStoreColumns.RATING, (long) rating);
            database.replaceOrThrow(RatingStoreColumns.NAME, null, values);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void updateRating(int songId, int rating) {
    }
}

abstract class RatingStoreColumns {
    static String NAME = "rating";
    static String ID = "songid";
    static String RATING = "songrating";
}