package com.ialexwantedi.yandexdisk;

import android.graphics.Bitmap;

/**
 * Класс объекта изображения.
 */
public class ImageItem {

    private String name, path, md5;
    private Bitmap bitmap;

    public ImageItem(String name, String path, String md5, Bitmap bitmap) {
        this.name = name;
        this.path = path;
        this.md5 = md5;
        this.bitmap = bitmap;
    }

    public ImageItem(String name, String path, Bitmap bitmap) {
        this.name = name;
        this.path = path;
        this.md5 = "";
        this.bitmap = bitmap;
    }

    /**
     * Получение имени изображения.
     * @return name - имя файла.
     */
    public String getName() {
        return name;
    }

    /**
     * Получение пути к изображению.
     * @return path - путь к изображению.
     */
    public String getPath() {
        return path;
    }

    /**
     * Получение MD5 суммы изображения.
     * @return md5 - MD5 сумма изображения.
     */
    public String getMD5() {
        return md5;
    }

    /**
     * Получение Bitmap изображения.
     * @return bitmap - Bitmap изображения.
     */
    public Bitmap getBitmap() {
        return bitmap;
    }
}
