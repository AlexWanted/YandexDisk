package com.ialexwantedi.yandexdisk;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ProgressListener;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Класс Activity просмотра изображения во весь экран.
 */
public class ImageFullscreenActivity extends AppCompatActivity {

    ImageView fullscreenImage;
    Credentials credentials;
    RestClient client;
    File image;
    Bitmap bitmap;
    boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_fullscreen);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        setStatusBarBackground();
        credentials = new Credentials("", getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Constants.PREF_TOKEN, ""));
        client = new RestClient(credentials);
        fullscreenImage = (ImageView) findViewById(R.id.fullscreen_image);
        ((TextView) findViewById(R.id.image_name)).setText(getIntent().getStringExtra("Name"));
        ((LinearLayout) findViewById(R.id.arrow_back)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        isConnected = isConnected();
        new Task().execute();
    }

    /**
     * Изменение цвета у Status Bar.
     */
    protected void setStatusBarBackground(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.black_overlay));
        }
    }

    /**
     * Закрытие Activity по нажатию кнопки "Назад".
     */
    @Override
    public void onBackPressed() {
        finish();
    }

    /**
     * Проверка наличия подключения к сети.
     */
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = null;
        if (cm != null) netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    class Task extends AsyncTask<Void, Void, Void> implements ProgressListener {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                SQLiteHandler handler = new SQLiteHandler(getApplicationContext());
                SQLiteDatabase db = handler.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT * FROM "+SQLiteHandler.TABLE_MD5
                        +" WHERE "+SQLiteHandler.KEY_NAME_MD5 + " = ?",
                        new String[]{getIntent().getStringExtra("Name")});

                image = new File(getApplicationContext().getCacheDir(), getIntent().getStringExtra("Name"));


                /*
                  Скачивание изображения, если его нет в кэше либо если не совпадает
                  MD5 сумма уже существующего изображения и того, который на сервере.
                  Просто создание Bitmap изображения, если оно уже скачан.
                */

                if ((!image.exists() || cursor.moveToFirst() &&
                        !cursor.getString(cursor.getColumnIndex(SQLiteHandler.KEY_MD5))
                                .equals(getIntent().getStringExtra("MD5"))) && isConnected) {

                    client.downloadFile(getIntent().getStringExtra("Path"), image, this);
                    bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
                    FileOutputStream out = new FileOutputStream(image, false);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
                } else {
                    bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
                }

                db.close();
                cursor.close();
            }
            catch (IOException e) { e.printStackTrace(); }
            catch (ServerIOException e) { e.printStackTrace(); }
            catch (ServerException e) { e.printStackTrace(); }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            fullscreenImage.setImageBitmap(bitmap);
            ((ProgressBar) findViewById(R.id.progress_bar)).setVisibility(View.GONE);
        }

        @Override
        public void updateProgress(long loaded, long total) {}
        @Override
        public boolean hasCancelled() { return false; }
    }
}
