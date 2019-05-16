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
import com.naman14.timber.provider.RatingStore;
import com.naman14.timber.provider.RatingStoreColumns;
import com.naman14.timber.utils.PreferencesUtility;
import com.naman14.timber.utils.SortOrder;

import java.util.ArrayList;

public class ArtistSongLoader {

    public static ArrayList<Song> getSongsForArtist(Context context, long artistID) {
        Cursor cursor = makeArtistSongCursor(context, artistID);
        ArrayList songsList = new ArrayList();
        if ((cursor != null) && (cursor.moveToFirst()))
            do {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String artist = cursor.getString(2);
                String album = cursor.getString(3);
                int duration = cursor.getInt(4);
                int trackNumber = cursor.getInt(5);
                long albumId = cursor.getInt(6);
                long artistId = artistID;

                songsList.add(new Song(id, albumId, artistID, title, artist, album, duration, trackNumber));
            }
            while (cursor.moveToNext());
        if (cursor != null)
            cursor.close();
        return songsList;
    }


    public static Cursor makeArtistSongCursor(Context context, long artistID) {
        ContentResolver contentResolver = context.getContentResolver();
        final String artistSongSortOrder = PreferencesUtility.getInstance(context).getArtistSongSortOrder();
        if (artistSongSortOrder.equals("rating")) {
            return makeArtistSongRatingCursor(context, artistID);
        }
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String string = "is_music=1 AND title != '' AND artist_id=" + artistID;
        return contentResolver.query(uri, new String[]{"_id", "title", "artist", "album", "duration", "track", "album_id"}, string, null, artistSongSortOrder);
    }

    private static Cursor makeArtistSongRatingCursor(Context context, long artistID) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String string = "is_music=1 AND title != '' AND artist_id=" + artistID;
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"_id", "title", "artist", "album", "duration", "track", "album_id"}, string, null, SortOrder.AlbumSortOrder.ALBUM_A_Z);

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
                + MediaStore.Audio.AudioColumns.ALBUM_ID + " LONG NOT NULL);"
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
            contentValues.put("album_id", cursor.getLong(6));
            db.insert("temp_table", null, contentValues);
            cursor.moveToNext();
        }

        cursor.close();

        String query = "SELECT _id, title, artist, album, duration, track, album_id" +
                " FROM temp_table LEFT JOIN " + RatingStoreColumns.NAME +
                " ON temp_table._id = " + RatingStoreColumns.ID +
                " ORDER BY IFNULL(" + RatingStoreColumns.RATING + "," + RatingStore.DEFAULT + ") DESC;";

        return db.rawQuery(query, null);
    }
}
