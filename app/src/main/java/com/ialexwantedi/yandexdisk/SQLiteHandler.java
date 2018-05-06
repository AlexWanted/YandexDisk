package com.ialexwantedi.yandexdisk;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHandler extends SQLiteOpenHelper {

    static final int DB_VERSION = 1;
    static final String DB_NAME = "last_saved_response";

    public static final String  TABLE_IMAGES = "Images",
                                KEY_ID = "id",
                                KEY_PATH = "path",
                                KEY_NAME = "name";

    public static final String  TABLE_MD5 = "ImagesMD5",
                                KEY_ID_MD5 = "id",
                                KEY_NAME_MD5 = "name",
                                KEY_MD5 = "md5";

    public SQLiteHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL( "CREATE TABLE " + TABLE_IMAGES + " ("
                    + KEY_ID +      " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + KEY_NAME +    " TEXT, "
                    + KEY_PATH +    " TEXT )");

        db.execSQL( "CREATE TABLE " + TABLE_MD5 + " ("
                + KEY_ID_MD5 +  " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_NAME_MD5 +" TEXT, "
                + KEY_MD5 +     " TEXT )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) { }

    public void addRowToImages(String name, String path){
        ContentValues cv = new ContentValues();
        SQLiteDatabase db = this.getWritableDatabase();
        cv.put(KEY_NAME, name);
        cv.put(KEY_PATH, path);
        db.insert(TABLE_IMAGES, null, cv);
    }

    public void addRowToMD5(String name, String md5){
        ContentValues cv = new ContentValues();
        SQLiteDatabase db = this.getWritableDatabase();
        cv.put(KEY_NAME_MD5, name);
        cv.put(KEY_MD5, md5);
        db.insert(TABLE_MD5, null, cv);
    }

    public void dropImages(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_IMAGES, null, null);
    }
}
