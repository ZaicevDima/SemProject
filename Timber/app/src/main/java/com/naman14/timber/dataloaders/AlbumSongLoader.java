/*
 * Copyright (C) 2015 Naman Dwivedi
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package com.naman14.timber.dataloaders;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;

import com.naman14.timber.models.Song;
import com.naman14.timber.provider.MusicDB;
import com.naman14.timber.provider.RatingStoreColumns;
import com.naman14.timber.utils.PreferencesUtility;
import com.naman14.timber.utils.SortOrder;

import java.util.ArrayList;

public class AlbumSongLoader {

    private static final long[] sEmptyList = new long[0];

    public static ArrayList<Song> getSongsForAlbum(Context context, long albumID) {

        Cursor cursor = makeAlbumSongCursor(context, albumID);
        ArrayList arrayList = new ArrayList();
        if ((cursor != null) && (cursor.moveToFirst()))
            do {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String artist = cursor.getString(2);
                String album = cursor.getString(3);
                int duration = cursor.getInt(4);
                int trackNumber = cursor.getInt(5);
                /*This fixes bug where some track numbers displayed as 100 or 200*/
                while (trackNumber >= 1000) {
                    trackNumber -= 1000; //When error occurs the track numbers have an extra 1000 or 2000 added, so decrease till normal.
                }
                long artistId = cursor.getInt(6);
                long albumId = albumID;

                arrayList.add(new Song(id, albumId, artistId, title, artist, album, duration, trackNumber));
            }
            while (cursor.moveToNext());
        if (cursor != null)
            cursor.close();
        return arrayList;
    }

    private static Cursor makeAlbumSongCursor(Context context, long albumID) {
        ContentResolver contentResolver = context.getContentResolver();
        final String albumSongSortOrder = PreferencesUtility.getInstance(context).getAlbumSongSortOrder();

        if (albumSongSortOrder.equals("rating")) {
            return makeAlbumSongRatingCursor(context, albumID);
        }

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String string = "is_music=1 AND title != '' AND album_id=" + albumID;
        Cursor cursor = contentResolver.query(uri, new String[]{"_id", "title", "artist", "album", "duration", "track", "artist_id"}, string, null, albumSongSortOrder);
        return cursor;
    }

    private static Cursor makeAlbumSongRatingCursor(Context context, long albumID) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String string = "is_music=1 AND title != '' AND album_id=" + albumID;
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"_id", "title", "artist", "album", "duration", "track", "artist_id"}, string, null, SortOrder.AlbumSortOrder.ALBUM_A_Z);

        MusicDB musicDB = MusicDB.getInstance(context);
        SQLiteDatabase db = musicDB.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS temp_table");
        db.execSQL("CREATE TEMP TABLE IF NOT EXISTS " + "temp_table" + " ("
                + MediaStore.Audio.AudioColumns._ID + " LONG NOT NULL PRIMARY KEY,"
                + MediaStore.Audio.AudioColumns.TITLE + " STRING NOT NULL,"
                + MediaStore.Audio.AudioColumns.ARTIST + " STRING NOT NULL,"
                + MediaStore.Audio.AudioColumns.ALBUM + " STRING NOT NULL,"
                + MediaStore.Audio.AudioColumns.DURATION + " LONG NOT NULL,"
                + MediaStore.Audio.AudioColumns.TRACK + " STRING NOT NULL,"
                + MediaStore.Audio.AudioColumns.ARTIST_ID + " LONG NOT NULL);"
        );
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("_id", cursor.getLong(0));
            contentValues.put("title", cursor.getString(1));
            contentValues.put("artist", cursor.getString(2));
            contentValues.put("album", cursor.getString(3));
            contentValues.put("duration", cursor.getLong(4));
            contentValues.put("track", cursor.getString(5));
            contentValues.put("artist_id", cursor.getLong(6));
            db.insert("temp_table", null, contentValues);
            cursor.moveToNext();
        }

        cursor.close();

        String query = "SELECT _id, title, artist, album, duration, track, artist_id" +
                " FROM temp_table LEFT JOIN " + RatingStoreColumns.NAME +
                " ON temp_table._id = " + RatingStoreColumns.ID +
                " ORDER BY IFNULL(" + RatingStoreColumns.RATING + ", 5) DESC;";

        return db.rawQuery(query, null);
    }
}