package com.ialexwantedi.yandexdisk;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.json.ResourceList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс главной Activity с лентой изображений.
 */
public class HomePageActivity extends AppCompatActivity implements ClickListener {

    Credentials credentials;
    RestClient client;
    SQLiteHandler handler;
    ImageAdapter adapter;
    List<ImageItem> imageItemList;
    RecyclerView recyclerView;
    NestedScrollView scrollView;
    int totalImagesCount = 0, offset = 0;
    boolean needToUpdate = false, isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        /* Получение токена и сохранение его в SharedPreferences. */
        if (getIntent() != null && getIntent().getDataString() != null) {
            Pattern pattern = Pattern.compile("access_token=(.*?)(&|$)");
            Matcher matcher = pattern.matcher(getIntent().getDataString());
            if(matcher.find()){
                final String token = matcher.group(1);
                if (!TextUtils.isEmpty(token)) {
                    getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString(Constants.PREF_TOKEN, token)
                            .apply();
                    credentials = new Credentials("", token);
                }
            }
        } else {
            credentials = new Credentials("",
                            getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                            .getString(Constants.PREF_TOKEN, ""));
        }

        client = new RestClient(credentials);
        imageItemList = new ArrayList<>();
        initRecycler();
        isConnected = isConnected();
        handler = new SQLiteHandler(getApplicationContext());
        if (isConnected) {
            handler.dropImages();
            new Task().execute(0);
        } else {
            populateImagesListOffline();
        }

    }

    /**
     * Иницилизация RecyclerView и NestedScrollView
     * Добавление NestedScrollView ScrollListener'а,
     * для прогрузки новых элементов при прокрутке до конца страницы.
     */
    private void initRecycler() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        scrollView = (NestedScrollView) findViewById(R.id.scrollView);
        GridLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 3);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(3, 6, false));
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ImageAdapter(imageItemList);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (scrollView != null && recyclerView.getChildAt(1) != null) {
                    if (scrollView.getChildAt(0).getBottom() <= (scrollView.getHeight() + scrollView.getScrollY()
                            + recyclerView.getChildAt(1).getHeight())) {
                        if (totalImagesCount == Constants.IMAGES_TOTAL && !needToUpdate) {
                            needToUpdate = true;
                            offset += Constants.IMAGES_TOTAL;
                            new Task().execute(offset);
                        }
                    }
                }
            }
        });
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

    /**
     * Создание списка изображений из последнего сохранённого ответа сервера
     * в случае, если отсутсвует подключение.
     */
    private void populateImagesListOffline() {
        SQLiteDatabase db = handler.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "+SQLiteHandler.TABLE_IMAGES, null);
        if (cursor.moveToFirst()) {
            int idName = cursor.getColumnIndex(SQLiteHandler.KEY_NAME);
            int idPath = cursor.getColumnIndex(SQLiteHandler.KEY_PATH);
            ImageItem imageItem;
            Bitmap bitmap;
            do {
                String imageName = cursor.getString(idName);
                String imagePath = cursor.getString(idPath);
                File preview = new File(getApplicationContext().getCacheDir(), "preview_" + imageName);
                bitmap = BitmapFactory.decodeFile(preview.getAbsolutePath());
                imageItem = new ImageItem(imageName, imagePath, bitmap);
                imageItemList.add(imageItem);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            } while (cursor.moveToNext());
        }
        db.close();
        cursor.close();
    }

    /**
     * Обработка нажатия на отдельный элемент RecyclerView.
     * При нажатии открывается отдельная Activity для просмотра изображения.
     */
    @Override
    public void itemClicked(View view, int position) {
        Intent intent = new Intent(this, ImageFullscreenActivity.class);
        intent.putExtra("Name", imageItemList.get(position).getName());
        intent.putExtra("Path", imageItemList.get(position).getPath());
        intent.putExtra("MD5", imageItemList.get(position).getMD5());
        startActivity(intent);
    }

    /**
     * Скачивание превью с сервера.
     * @param previewLink - ссылка на превью.
     * @param previewFile - оьект файла превью.
     */
    private Bitmap downloadPreview(String previewLink, File previewFile) throws IOException {
        URL obj = new URL(previewLink);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization",
                "OAuth " + getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                        .getString(Constants.PREF_TOKEN, ""));
        InputStream is = connection.getInputStream();
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        FileOutputStream out = new FileOutputStream(previewFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        return bitmap;
    }

    class Task extends AsyncTask<Integer, Void, Void> {

        @Override
        protected void onPreExecute() { super.onPreExecute(); }
        @Override
        protected Void doInBackground(Integer... params) {
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((ProgressBar) findViewById(R.id.progress_bar)).setVisibility(View.VISIBLE);
                    }
                });

                SQLiteDatabase db = handler.getReadableDatabase();

                /* Получение списка ресурсов */
                ResourceList resourceList = client.getFlatResourceList(new ResourcesArgs.Builder()
                        .setPreviewSize("M")
                        .setMediaType("image")
                        .setLimit(Constants.IMAGES_TOTAL)
                        .setOffset(params[0])
                        .build());
                totalImagesCount = resourceList.getItems().size();

                for (Resource res : resourceList.getItems()) {
                    Cursor cursor = db.rawQuery("SELECT * FROM "+SQLiteHandler.TABLE_MD5
                            +" WHERE "+SQLiteHandler.KEY_NAME_MD5 + " = ?",
                            new String[]{res.getName()});
                    int md5Index = cursor.getColumnIndex(SQLiteHandler.KEY_MD5);
                    String md5 = "";
                    if (cursor.moveToFirst()) md5 = cursor.getString(md5Index);
                    File preview = new File(getApplicationContext().getCacheDir(), "preview_" + res.getName());
                    Bitmap bitmap;

                    /*
                      Скачивание превью, если её нет в кэше либо если не совпадает
                      MD5 сумма уже существующей превью и той, которая на сервере.
                      Просто создание Bitmap превью, если она уже скачана.
                     */
                    if (!preview.exists() || cursor.moveToFirst() && !md5.equals(res.getMd5())) {
                        bitmap = downloadPreview(res.getPreview(), preview);
                    } else {
                        bitmap = BitmapFactory.decodeFile(preview.getAbsolutePath());
                    }

                    /* Заполнение imageItemList объектами изображений. */
                    ImageItem imageItem = new ImageItem(res.getName(), res.getPath().toString(), res.getMd5(), bitmap);
                    imageItemList.add(imageItem);
                    handler.addRowToImages(res.getName(), res.getPath().toString());
                    handler.addRowToMD5(res.getName(), res.getMd5());
                    cursor.close();
                }
                db.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ServerIOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            adapter.notifyDataSetChanged();
            ((ProgressBar) findViewById(R.id.progress_bar)).setVisibility(View.GONE);
            needToUpdate = false;
        }
    }

    /**
     * Класс кастомного декоратора для RecyclerView.
     */
    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect rect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                rect.left = spacing - column * spacing / spanCount;
                rect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    rect.top = spacing;
                }
                rect.bottom = spacing;
            } else {
                rect.left = column * spacing / spanCount;
                rect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    rect.top = spacing;
                }
            }
        }
    }
}
