package ai.nexconn.chatui.utils.file;

import ai.nexconn.chatui.utils.log.RLog;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.MimeTypeMap;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileUtils {
    private static String TAG = "FileUtils";
    private static final String CREATED_FOLDERS_FAIL = "Created folders unSuccessfully";
    public static final int FILE_SCHEME_LENGTH = 7;
    private static final int INT_4 = 4;

    private static final HashMap<String, String> FILE_TYPE_MAP = new HashMap<>();

    static {
        // JPEG, jpg
        FILE_TYPE_MAP.put("FFD8FF", "jpg");
        FILE_TYPE_MAP.put("89504E", "png");
        FILE_TYPE_MAP.put("47494638", "gif");
        FILE_TYPE_MAP.put("52494646", "webp");
        // Matroska stream file (mkv)
        FILE_TYPE_MAP.put("1A45DFA3934282886D6174726F736B61", "mkv");
        // MPEG-4 video files (mp4)
        FILE_TYPE_MAP.put("000000", "mp4");
        FILE_TYPE_MAP.put("255044", "pdf");

        // TIFF, tif
        FILE_TYPE_MAP.put("49492A", "tif");
        // 16-color bitmap (bmp)
        FILE_TYPE_MAP.put("424d22", "bmp");
        // 24-bit bitmap (bmp)
        FILE_TYPE_MAP.put("424d82", "bmp");
        // 256-color bitmap (bmp)
        FILE_TYPE_MAP.put("424d8e", "bmp");
        FILE_TYPE_MAP.put("414331", "dwg");
        FILE_TYPE_MAP.put("3C2144", "htm");
        // HyperText Markup Language 3 (html)
        FILE_TYPE_MAP.put("3C21444F4354", "html");
        FILE_TYPE_MAP.put("48544d", "css");
        FILE_TYPE_MAP.put("696b2e", "js");
        // Rich Text Format (rtf)
        FILE_TYPE_MAP.put("7B5C72", "rtf");
        // Photoshop (psd)
        FILE_TYPE_MAP.put("384250", "psd");
        // Icon File (ico)
        FILE_TYPE_MAP.put("00000100", "ico");
        // Email (eml)
        FILE_TYPE_MAP.put("44656C69766572792D646174653A", "eml");
        // MS Access (mdb, mda, mde, mdt)
        FILE_TYPE_MAP.put("5374616E64617264204A", "mdb");
        // Postscript (ps, eps)
        FILE_TYPE_MAP.put("252150532D41646F6265", "ps");
        // Adobe EPS File (eps)
        FILE_TYPE_MAP.put("25215053", "eps");
        // Real Media streaming media file (rm, rmvb)
        FILE_TYPE_MAP.put("2E524D46", "rmvb");
        FILE_TYPE_MAP.put("2E524D", "rm");
        // Flash video file (flv, f4v)
        FILE_TYPE_MAP.put("464C5601", "flv");
        // MPEG-1 Audio Layer 3 (MP3) audio file (mp3)
        FILE_TYPE_MAP.put("494433", "mp3");
        // MPEG (mpg)
        FILE_TYPE_MAP.put("000001BA", "mpg");
        // MPEG Movie (mpg, mpeg)
        FILE_TYPE_MAP.put("000001B3", "mpg");
        FILE_TYPE_MAP.put("3026b2", "wmv");
        // Windows Media (asf)
        FILE_TYPE_MAP.put("3026B2758E66CF11", "asf");
        // Wave (wav)
        FILE_TYPE_MAP.put("57415645", "wav");
        // Audio Video Interleave (avi)
        FILE_TYPE_MAP.put("41564920", "avi");
        // Musical Instrument Digital Interface (MIDI) sound file (MID, MIDI)
        FILE_TYPE_MAP.put("4D546864", "mid");
        // ZIP Archive (zip, jar, zipx)
        FILE_TYPE_MAP.put("504B3030504B0304", "zip");
        // RAR Archive File (rar)
        FILE_TYPE_MAP.put("52617221", "rar");
        FILE_TYPE_MAP.put("235468", "ini");
        // EXE, DLL, OCX, OLB, IMM, IME
        FILE_TYPE_MAP.put("4d5a90", "exe");
        FILE_TYPE_MAP.put("3c2540", "jsp");
        FILE_TYPE_MAP.put("4d616e", "mf");
        // XML Document (xml)
        FILE_TYPE_MAP.put("3C3F786D6C", "xml");
        FILE_TYPE_MAP.put("494e53", "sql");
        FILE_TYPE_MAP.put("706163", "java");
        FILE_TYPE_MAP.put("406563", "bat");
        // Gzip Archive File (gz, tar, tgz)
        FILE_TYPE_MAP.put("1f8b", "gz");
        FILE_TYPE_MAP.put("6c6f67", "properties");
        FILE_TYPE_MAP.put("cafeba", "class");
        // Microsoft Compiled HTML Help File (chm)
        FILE_TYPE_MAP.put("49545346", "chm");
        FILE_TYPE_MAP.put("040000", "mxp");
        FILE_TYPE_MAP.put("643130", "torrent");
        // Quicktime (mov)
        FILE_TYPE_MAP.put("6D6F6F76", "mov");
        // WordPerfect (wpd)
        FILE_TYPE_MAP.put("FF575043", "wpd");
        // Outlook Express (dbx)
        FILE_TYPE_MAP.put("CFAD12FEC5FD746F", "dbx");
        // Microsoft Outlook Personal Folder file (pst)
        FILE_TYPE_MAP.put("2142444E", "pst");
        // Quicken (qdf)
        FILE_TYPE_MAP.put("AC9EBD8F", "qdf");
        // Windows Password (pwl)
        FILE_TYPE_MAP.put("E3828596", "pwl");
        // Real Audio File (ra, ram)
        FILE_TYPE_MAP.put("2E7261FD", "ram");
    }

    private FileUtils() {}

    /**
     * Creates an input stream for reading the specified file into memory.
     *
     * @param path file path
     * @return input stream for the file
     */
    public static InputStream getFileInputStream(String path) {
        FileInputStream fileInputStream = null;
        if (!TextUtils.isEmpty(path)) {
            try {
                fileInputStream = new FileInputStream(new File(path));
            } catch (FileNotFoundException e) {
                RLog.e(TAG, "getFileInputStream", e);
            }
        }
        return fileInputStream;
    }

    /**
     * Reads a file as bytes, commonly used for binary files such as images, audio, and video.
     *
     * @param uri file URI
     * @return byte array read from the file
     */
    public static byte[] getByteFromUri(Uri uri) {
        if (uri == null) {
            RLog.e(TAG, "getByteFromUri uri should not be null!");
            return new byte[0];
        }
        InputStream input = getFileInputStream(uri.getPath());
        if (input == null) {
            RLog.e(TAG, "getByteFromUri getFileInputStream null!");
            return new byte[0];
        }
        try {
            int count = 0;
            while (count == 0) {
                count = input.available();
                if (count == 0) break;
            }

            byte[] bytes = new byte[count];
            int byteCount = input.read(bytes);
            RLog.i(TAG, "byteCount = " + byteCount);
            return bytes;
        } catch (Exception e) {
            return new byte[0];
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                RLog.e(TAG, "getByteFromUri", e);
            }
        }
    }

    /**
     * Writes byte array sequentially to a file output stream.
     *
     * @param uri file Uri
     * @param data byte array
     */
    public static void writeByte(Uri uri, byte[] data) {
        if (uri == null || uri.getPath() == null || uri.getPath().lastIndexOf("/") == -1) {
            return;
        }

        File fileFolder = new File(uri.getPath().substring(0, uri.getPath().lastIndexOf("/")));
        boolean successMkdir = fileFolder.mkdirs();
        if (!successMkdir) {
            RLog.e(TAG, CREATED_FOLDERS_FAIL);
        }

        File file = new File(uri.getPath());
        try (FileOutputStream fot = new FileOutputStream(file);
                OutputStream os = new BufferedOutputStream(fot)) {
            os.write(data);
        } catch (IOException ex) {
            RLog.e(TAG, "writeByte", ex);
        }
    }

    /**
     * Converts a bitmap to a file.
     *
     * @param bm bitmap
     * @param dir storage directory path
     * @param name file name
     * @return the output file
     */
    public static File convertBitmap2File(Bitmap bm, String dir, String name) {
        if (bm == null || TextUtils.isEmpty(dir)) {
            RLog.e(TAG, "convertBitmap2File bm or dir should not be null!");
            return null;
        }
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            RLog.e(TAG, "convertBitmap2File: dir does not exist! -" + dirFile.getAbsolutePath());
            boolean successMkdir = dirFile.mkdirs();
            if (!successMkdir) {
                RLog.e(TAG, CREATED_FOLDERS_FAIL);
            }
        }
        File targetFile = new File(dirFile.getPath() + File.separator + name);
        if (targetFile.exists()) {
            boolean isDelete = targetFile.delete();
            RLog.e(TAG, "convertBitmap2File targetFile isDelete:" + isDelete);
        }
        File tmpFile = new File(dirFile.getPath() + File.separator + name + ".tmp");

        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
            bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            RLog.e(TAG, "convertBitmap2File: Exception!", e);
        }
        targetFile = new File(dirFile.getPath() + File.separator + name);
        if (tmpFile.renameTo(targetFile)) {
            return targetFile;
        } else {
            return tmpFile;
        }
    }

    /**
     * Copies a file.
     *
     * @param src source file
     * @param path destination directory path
     * @param name destination file name
     * @return the destination file
     */
    public static File copyFile(File src, String path, String name) {
        if (src == null) {
            RLog.e(TAG, "copyFile src should not be null!");
            return null;
        }
        File dest;
        if (!src.exists()) {
            RLog.e(TAG, "copyFile: src file does not exist! -" + src.getAbsolutePath());
            return null;
        }

        dest = new File(path);
        if (!dest.exists()) {
            RLog.d(TAG, "copyFile: dir does not exist!");
            boolean successMkdir = dest.mkdirs();
            if (!successMkdir) {
                RLog.e(TAG, CREATED_FOLDERS_FAIL);
            }
        }
        dest = new File(path + name);

        try (FileInputStream fis = new FileInputStream(src);
                FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
        } catch (IOException ex) {
            RLog.e(TAG, "copyFile: Exception!", ex);
            return dest;
        }
        return dest;
    }

    /**
     * Copies a file. Only supports file:// scheme paths.
     *
     * @param srcPath source file path
     * @param path destination directory path
     * @param name destination file name
     * @return whether the copy was successful
     */
    public static boolean copyFile(String srcPath, String path, String name) {
        if (TextUtils.isEmpty(srcPath)) {
            RLog.e(TAG, "copyFile src should not be null!");
            return false;
        }
        File src = new File(srcPath);
        File dest;
        if (!src.exists()) {
            RLog.e(TAG, "copyFile: src file does not exist! -" + src.getAbsolutePath());
            return false;
        }

        dest = new File(path);
        if (!dest.exists()) {
            RLog.d(TAG, "copyFile: dir does not exist!");
            boolean successMkdir = dest.mkdirs();
            if (!successMkdir) {
                RLog.e(TAG, CREATED_FOLDERS_FAIL);
            }
        }
        dest = new File(path, name);
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
        } catch (IOException e) {
            RLog.e(TAG, "copyFile: Exception!", e);
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "copyFile fos close", e);
            }
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "copyFile fis close", e);
            }
        }

        return true;
    }

    /**
     * @param context context
     * @param srcUri source URI starting with file:// or content://
     * @param desPath destination path under the app's package directory
     * @return whether the file was copied successfully
     */
    public static boolean copyFileToInternal(
            Context context, Uri srcUri, String desPath, String name) {
        try {
            File dir = new File(desPath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    RLog.d(TAG, "create dir failed, path is" + desPath);
                    return false;
                }
            }
        } catch (Exception e) {
            RLog.d(TAG, "create dir exception, path is" + desPath);
            return false;
        }
        if (uriStartWithFile(srcUri)) {
            return copyFile(srcUri.toString().substring(7), desPath, name);
        } else if (uriStartWithContent(srcUri)) {
            return copyFile(context, srcUri, new File(desPath, name).getAbsolutePath());
        } else {
            return copyFile(srcUri != null ? srcUri.toString() : "", desPath, name);
        }
    }

    /**
     * @param context context
     * @param srcUri content:// URI obtained from MediaStore
     * @param desPath destination path under the app's package directory
     * @return whether the file was copied successfully
     */
    public static boolean copyFile(Context context, Uri srcUri, String desPath) {
        if (!uriStartWithContent(srcUri)) return false;
        InputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = context.getContentResolver().openInputStream(srcUri);
            fos = new FileOutputStream(desPath);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
        } catch (IOException e) {
            RLog.e(TAG, "copyFile: Exception!", e);
            return false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException pE) {
                    RLog.e(TAG, "copyFile: Exception!", pE);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException pE) {
                    RLog.e(TAG, "copyFile: Exception!", pE);
                }
            }
        }
        return true;
    }

    /**
     * Gets the file name from a file path.
     *
     * @param path file path
     * @return file name
     */
    public static String getFileNameWithPath(String path) {
        if (TextUtils.isEmpty(path)) {
            RLog.e(TAG, "getFileNameWithPath path should not be null!");
            return null;
        }
        int start = path.lastIndexOf("/");
        if (start != -1) {
            return path.substring(start + 1);
        } else {
            return null;
        }
    }

    /**
     * Gets the byte array of the specified file.
     *
     * @param file the file
     * @return byte array of the file
     */
    public static byte[] file2byte(File file) {
        if (file == null) {
            RLog.e(TAG, "file2byte file should not be null!");
            return new byte[0];
        }
        if (!file.exists()) {
            RLog.e(TAG, "file2byte: src file does not exist! -" + file.getAbsolutePath());
            return new byte[0];
        }

        byte[] buffer = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            bos.close();
            buffer = bos.toByteArray();
        } catch (Exception e1) {
            RLog.e(TAG, "file2byte: Exception!", e1);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    RLog.e(TAG, "file2byte: fis close!", e);
                }
            }
        }
        return buffer;
    }

    public static byte[] file2byte(Context context, Uri uri) {
        if (FileUtils.uriStartWithFile(uri)) {
            String path = uri.toString().substring(7);
            return file2byte(new File(path));
        } else if (FileUtils.uriStartWithContent(uri)) {
            return contentFile2byte(context, uri);
        } else {
            return new byte[0];
        }
    }

    /**
     * Gets the byte array of the specified file. The URI must start with content://.
     *
     * @param context context
     * @param uri content:// URI
     * @return byte array of the file
     */
    public static byte[] contentFile2byte(Context context, Uri uri) {
        if (context == null || uri == null || !FileUtils.uriStartWithContent(uri)) {
            RLog.e(TAG, "contentFile2byte params error");
            return new byte[0];
        }
        ParcelFileDescriptor r;
        try {
            r = context.getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            RLog.e(TAG, "contentFile2byte file not found uri is " + uri.toString());
            return new byte[0];
        }
        byte[] buffer = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(r.getFileDescriptor());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            bos.close();
            buffer = bos.toByteArray();
        } catch (Exception e1) {
            RLog.e(TAG, "file2byte: Exception!", e1);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "file2byte fis close", e);
            }
        }
        return buffer;
    }

    /**
     * Creates a file from a byte array.
     *
     * @param buf byte array
     * @param filePath destination directory path
     * @param fileName file name
     * @return the created file
     */
    public static File byte2File(byte[] buf, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists()) {
                RLog.d(TAG, "byte2File: dir does not exist!");
                boolean successMkdir = dir.mkdirs();
                if (!successMkdir) {
                    RLog.e(TAG, CREATED_FOLDERS_FAIL);
                }
            }
            file = new File(dir.getPath() + File.separator + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(buf);
        } catch (Exception e) {
            RLog.e(TAG, "byte2File: Exception!", e);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    RLog.e(TAG, "byte2File: IOException!", e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    RLog.e(TAG, "byte2File: IOException!", e);
                }
            }
        }
        return file;
    }

    /**
     * Gets the cache storage path, e.g. {@code /sdcard/Android/data/<package name>/cache}
     *
     * @param context Context
     * @return full directory path, or empty string "" if external storage is not mounted or other
     *     error occurs
     */
    public static String getCachePath(Context context) {
        return getCachePath(context, "");
    }

    private static boolean hasFilePermission(Context context) {
        return context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Gets the key for a media message, used for image, video, GIF message handlers.
     *
     * @param message message
     * @return media message key for local storage
     */
    public static String getFileKey(Object message) {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * Gets the cache storage path, e.g. {@code /sdcard/Android/data/<package name>/cache/<dir>}
     *
     * @param context Context
     * @param dir custom directory name
     * @return full directory path, or empty string "" if external storage is not mounted or other
     *     error occurs
     */
    public static String getCachePath(Context context, String dir) {
        boolean sdCardExist = false;
        try {
            sdCardExist = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        } catch (Exception e) {
            RLog.e(TAG, "getCachePath", e);
        }

        File cacheDir;
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
                || hasFilePermission(context)) {
            cacheDir = getExternalCacheDir(context);
        } else {
            cacheDir = context.getCacheDir();
        }
        if (!sdCardExist || cacheDir == null || (!cacheDir.exists() && !cacheDir.mkdirs())) {
            cacheDir = context.getCacheDir();
        }

        File tarDir = new File(cacheDir.getPath() + File.separator + dir);
        if (tarDir.exists() && tarDir.isFile()) {
            boolean isDelete = tarDir.delete();
            RLog.e(TAG, "getCachePath isDelete:" + isDelete);
        }

        if (!tarDir.exists()) {
            boolean result = tarDir.mkdir();
            RLog.w(TAG, "getCachePath = " + tarDir.getPath() + ", result = " + result);
            if (!result) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasFilePermission(context)) {
                    tarDir = new File("/sdcard/cache/" + dir);
                    if (!tarDir.exists()) result = tarDir.mkdirs();
                } else {
                    File filesDir = context.getFilesDir();
                    tarDir = new File(filesDir, dir);
                    if (!tarDir.exists()) {
                        result = tarDir.mkdirs();
                    }
                }
                RLog.e(TAG, "change path = " + tarDir.getPath() + ", result = " + result);
            }
        }
        return tarDir.getPath();
    }

    /**
     * Gets the cache storage path, e.g. {@code /sdcard/Android/data/<package name>/cache/<dir>}
     *
     * @param context Context
     * @param dir custom directory name (supports multi-level directories)
     * @return full directory path, or empty string "" if external storage is not mounted or other
     *     error occurs
     */
    public static String getCacheDirsPath(Context context, String dir) {
        boolean sdCardExist = false;
        try {
            sdCardExist = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        } catch (Exception e) {
            RLog.e(TAG, "getCacheDirsPath ", e);
        }

        File cacheDir;
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
                || hasFilePermission(context)) {
            cacheDir = getExternalCacheDir(context);
        } else {
            cacheDir = context.getCacheDir();
        }
        if (!sdCardExist || cacheDir == null || (!cacheDir.exists() && !cacheDir.mkdirs())) {
            cacheDir = context.getCacheDir();
        }

        File tarDir = new File(cacheDir.getPath() + File.separator + dir);
        if (tarDir.exists()) {
            if (tarDir.isFile()) {
                boolean isDelete = tarDir.delete();
                RLog.e(TAG, "getCacheDirsPath isDelete:" + isDelete);
            }
        } else {
            boolean successMkdir = tarDir.mkdirs();
            if (!successMkdir) {
                RLog.e(TAG, "getCacheDirsPath " + CREATED_FOLDERS_FAIL);
            }
        }

        if (!tarDir.exists()) {
            boolean result = tarDir.mkdir();
            RLog.w(TAG, "getCachePath = " + tarDir.getPath() + ", result = " + result);
            if (!result) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasFilePermission(context)) {
                    tarDir = new File("/sdcard/cache/" + dir);
                    if (!tarDir.exists()) result = tarDir.mkdirs();
                } else {
                    File filesDir = context.getFilesDir();
                    tarDir = new File(filesDir, dir);
                    if (!tarDir.exists()) {
                        result = tarDir.mkdirs();
                    }
                }
                RLog.e(TAG, "change path = " + tarDir.getPath() + ", result = " + result);
            }
        }
        return tarDir.getPath();
    }

    /**
     * Gets the temporary file storage path for resumable downloads.
     *
     * @param context context
     * @param messageId message ID
     * @return storage path
     */
    public static String getTempFilePath(Context context, int messageId) {
        return getTempFilePath(context, messageId + "");
    }

    /**
     * Gets the temporary file storage path for resumable downloads.
     *
     * @param context context
     * @param id unique file identifier
     * @return storage path
     */
    public static String getTempFilePath(Context context, String id) {
        String path = getCacheDirsPath(context, "tmp") + File.separator + id + ".txt";
        return path;
    }

    /**
     * Gets the app internal cache path {@code data/data/<package name>/cache/<dir>}
     *
     * @param context Context
     * @param dir custom directory name
     * @return full directory path
     */
    public static String getInternalCachePath(Context context, String dir) {
        if (context == null) {
            return null;
        }
        File cacheDir = new File(context.getCacheDir().getPath() + File.separator + dir);
        if (!cacheDir.exists()) {
            boolean result = cacheDir.mkdir();
            RLog.w(TAG, "getInternalCachePath = " + cacheDir.getPath() + ", result = " + result);
        }
        return cacheDir.getPath();
    }

    /**
     * Gets the media file storage directory.
     *
     * @param context context
     * @return media file storage path
     * @deprecated This method is deprecated.
     */
    @Deprecated
    public static String getMediaDownloadDir(Context context) {
        return LibStorageUtils.getMediaDownloadDir(context);
    }

    /**
     * Gets the media file storage directory.
     *
     * @param context context
     * @param dir custom directory name
     * @return media file storage path
     */
    public static String getMediaDownloadDir(Context context, String dir) {
        return LibStorageUtils.getMediaDownloadDir(context, dir);
    }

    /**
     * Gets the size of the specified file.
     *
     * @param file the file
     * @return file length
     */
    public static long getFileSize(File file) {
        long size = 0;
        FileInputStream fis = null;
        try {
            if (file.exists()) {
                fis = new FileInputStream(file);
                size = fis.available();
                if (fis != null) {
                    fis.close();
                }
            } else {
                RLog.d(TAG, "file doesn't exist");
            }
        } catch (Exception e) {
            RLog.e(TAG, "getFileSize", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    RLog.e(TAG, "getFileSize: fis close!", e);
                }
            }
        }
        return size;
    }

    /**
     * Saves a string to the specified file path.
     *
     * @param str the string to save
     * @param filePath destination file path
     */
    public static void saveFile(String str, String filePath) {
        FileOutputStream outStream = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                File dir = new File(file.getParent());
                boolean successMkdir = dir.mkdirs();
                if (!successMkdir) {
                    RLog.e(TAG, CREATED_FOLDERS_FAIL);
                }

                boolean isCreateNewFile = file.createNewFile();
                RLog.e(TAG, "saveFile isCreateNewFile" + isCreateNewFile);
            }
            outStream = new FileOutputStream(file);
            outStream.write(str.getBytes());
        } catch (Exception e) {
            RLog.e(TAG, "saveFile", e);
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    RLog.e(TAG, "saveFile: outStream close!", e);
                }
            }
        }
    }

    /**
     * Reads file content as a string.
     *
     * @param path file path
     * @return file content as string
     */
    public static String getStringFromFile(String path) {
        if (TextUtils.isEmpty(path)) {
            RLog.e(TAG, "getStringFromFile path should not be null!");
            return null;
        }
        File file = new File(path);
        if (!file.exists()) {
            RLog.e(TAG, "getStringFromFile file is not exists,path:" + path);
            return "";
        }

        FileInputStream in = null;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            in = new FileInputStream(path);
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } catch (IOException e) {
            RLog.e(TAG, "getStringFromFile IOException", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    RLog.e(TAG, "getStringFromFile IOException", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    RLog.e(TAG, "getStringFromFile: in close!", e);
                }
            }
        }
        return content.toString();
    }

    /**
     * Deletes a file.
     *
     * @param path file path
     */
    public static void removeFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                boolean isDelete = file.delete();
                RLog.e(TAG, "removeFile isDelete:" + isDelete);
            }
        } catch (Exception e) {
            RLog.e(TAG, "removeFile Exception", e);
        }
    }

    /**
     * Gets the MD5 key for resumable file download, used to identify the file being downloaded.
     *
     * @param context context
     * @param messageId message ID
     * @return the key
     */
    public static String getTempFileMD5(Context context, int messageId) {
        return getTempFileMD5(context, messageId + "");
    }

    /**
     * Gets the temporary file name for resumable download, used to identify the file being
     * downloaded.
     *
     * @param context context
     * @param tag unique file identifier
     * @return the key
     */
    public static String getTempFileMD5(Context context, String tag) {
        return tag;
    }

    /**
     * Reads the rotation degree of an image.
     *
     * @param context context
     * @param path absolute image path
     * @return rotation degree
     */
    public static int readPictureDegree(Context context, String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = null;
            if (LibStorageUtils.isBuildAndTargetForQ(context)) {
                if (FileUtils.uriStartWithContent(Uri.parse(path))) {
                    ParcelFileDescriptor pfd =
                            context.getContentResolver().openFileDescriptor(Uri.parse(path), "r");
                    if (pfd != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        exifInterface = new ExifInterface(pfd.getFileDescriptor());
                    }
                } else {
                    exifInterface = new ExifInterface(path);
                }
            } else {
                exifInterface = new ExifInterface(path);
            }
            if (exifInterface == null) {
                return 0;
            }
            int orientation =
                    exifInterface.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            RLog.e(TAG, "readPictureDegree error");
        }
        return degree;
    }

    /**
     * Checks whether a file exists.
     *
     * @param pContext context
     * @param pUri file Uri
     * @return {@code true} if the file exists, {@code false} otherwise
     */
    public static boolean isFileExistsWithUri(Context pContext, Uri pUri) {
        if (pUri == null) return false;
        if (uriStartWithFile(pUri)) {
            String subPath = pUri.toString().substring(FILE_SCHEME_LENGTH);
            return new File(subPath).exists();
        } else if (uriStartWithContent(pUri)) {
            InputStream is = null;
            try {
                is = pContext.getContentResolver().openInputStream(pUri);
                return is != null;
            } catch (Exception e) {
                return false;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        RLog.d(TAG, "isFileExistsWithUri IOException");
                    }
                }
            }
        } else {
            if (pUri.toString().startsWith("/")) {
                return new File(pUri.toString()).exists();
            } else {
                return false;
            }
        }
    }

    /**
     * Gets the file length. Returns -1 if the file does not exist.
     *
     * @param pContext context
     * @param pUri file Uri
     * @return file length, or -1 if not found
     */
    public static long getFileLengthWithUri(Context pContext, Uri pUri) {
        if (pUri == null) return -1;
        if (uriStartWithFile(pUri)) {
            String subPath = pUri.toString().substring(FILE_SCHEME_LENGTH);
            File file = new File(subPath);
            if (file.exists()) {
                return file.length();
            } else {
                return -1;
            }
        } else if (uriStartWithContent(pUri)) {
            InputStream is = null;
            try {
                is = pContext.getContentResolver().openInputStream(pUri);
                if (is != null) {
                    return is.available();
                } else {
                    return -1;
                }
            } catch (Exception e) {
                return -1;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            File file = new File(pUri.toString());
            if (file.exists()) {
                return file.length();
            } else {
                return -1;
            }
        }
    }

    /**
     * Checks whether the file Uri scheme starts with "file" or "content".
     *
     * @param pUri file Uri
     * @return true if the Uri scheme starts with "file" or "content"
     */
    public static boolean isValidateLocalUri(Uri pUri) {
        return uriStartWithFile(pUri) || uriStartWithContent(pUri);
    }

    /**
     * Checks whether the file Uri scheme starts with "file" and its length exceeds 7.
     *
     * @param pUri file Uri
     * @return true if the Uri scheme is "file" and its length exceeds 7
     */
    public static boolean uriStartWithFile(Uri pUri) {
        return pUri != null && "file".equals(pUri.getScheme()) && pUri.toString().length() > 7;
    }

    /**
     * Checks whether the file Uri scheme starts with "content".
     *
     * @param srcUri file Uri
     * @return true if the Uri scheme is "content"
     */
    public static boolean uriStartWithContent(Uri srcUri) {
        return srcUri != null && "content".equals(srcUri.getScheme());
    }

    /**
     * Gets the file extension from a Uri.
     *
     * @param srcUri file Uri
     * @return extension, may be null
     */
    public static String getSuffix(Uri srcUri) {
        if (srcUri == null) {
            return null;
        }
        return getSuffix(srcUri.toString());
    }

    /**
     * Gets the file extension from a path.
     *
     * @param path file path
     * @return extension, may be null
     */
    public static String getSuffix(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        int lastDot = path.lastIndexOf(".");
        if (lastDot < 0) return null;
        return path.substring(lastDot + 1);
    }

    /**
     * Gets file name, size, and type from a URI.
     *
     * @param context context
     * @param uri supports content:// and file:// schemes
     * @return FileInfo containing file name, size, and type
     */
    public static FileInfo getFileInfoByUri(Context context, Uri uri) {
        if (uriStartWithContent(uri)) {
            return getFileInfoByContent(context, uri);
        } else if (uriStartWithFile(uri)) {
            return getFileInfoByFile(uri);
        } else {
            return null;
        }
    }

    /**
     * Gets file name, size, and type from a file:// URI.
     *
     * @param uri file:// URI
     * @return FileInfo containing file name, size, and type
     */
    private static FileInfo getFileInfoByFile(Uri uri) {
        if (uriStartWithFile(uri)) {
            String filePath = uri.toString().substring(7);
            File file = new File(filePath);
            if (file.exists()) {
                FileInfo fileInfo = new FileInfo();
                String name = file.getName();
                fileInfo.setSize(file.length());
                fileInfo.setName(name);
                int lastDotIndex = name.lastIndexOf(".");
                if (lastDotIndex > 0) {
                    String fileSuffix = file.getName().substring(lastDotIndex + 1);
                    fileInfo.setType(fileSuffix);
                }
                return fileInfo;
            } else {
                return null;
            }
        } else {
            RLog.e(TAG, "getDocumentByFile uri is not file");
            return null;
        }
    }

    /**
     * Gets file name, size, and type from a File object.
     *
     * @param file the file
     * @return FileInfo containing file name, size, and type
     */
    public static FileInfo getFileInfoByFile(File file) {
        if (file != null) {
            if (file.exists()) {
                FileInfo fileInfo = new FileInfo();
                String name = file.getName();
                fileInfo.setSize(file.length());
                fileInfo.setName(name);
                int lastDotIndex = name.lastIndexOf(".");
                if (lastDotIndex > 0) {
                    String fileSuffix = file.getName().substring(lastDotIndex + 1);
                    fileInfo.setType(fileSuffix);
                }
                return fileInfo;
            } else {
                RLog.e(TAG, "getFileInfoByFile file is not exist");
                return null;
            }
        } else {
            RLog.e(TAG, "getFileInfoByFile file is null");
            return null;
        }
    }

    /**
     * Gets file name, size, and type from a content:// URI.
     *
     * @param context context
     * @param uri content:// URI
     * @return FileInfo containing file name, size, and type
     */
    private static FileInfo getFileInfoByContent(Context context, Uri uri) {
        if (uriStartWithContent(uri)) {
            String[] projection = {
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE
            };
            Cursor cursor = null;
            try {
                FileInfo fileInfo = new FileInfo();
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor == null) {
                    RLog.e(TAG, "getFileInfoByContent cursor is null");
                    return null;
                }
                if (cursor.moveToFirst()) {
                    if (cursor.getColumnCount() <= 0) {
                        return null;
                    }
                    String name = "";
                    long size = 0;
                    String mimeType = "";

                    int columnName = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (columnName >= 0) {
                        name = cursor.getString(columnName);
                        // Sanitize the file name to prevent path traversal attacks
                        name = FileUtils.sanitizeFilename(name);
                    } else {
                        RLog.e(TAG, "getFileInfoByContent columnIndex name");
                    }
                    int columnSize = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                    if (columnSize >= 0) {
                        size = cursor.getLong(columnSize);
                    } else {
                        RLog.e(TAG, "getFileInfoByContent columnIndex size");
                    }
                    int columnMimeType = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
                    if (columnMimeType >= 0) {
                        mimeType = cursor.getString(columnMimeType);
                    } else {
                        RLog.e(TAG, "getFileInfoByContent columnIndex mimeType");
                    }
                    fileInfo.setName(name);
                    fileInfo.setSize(size);
                    if (name != null && !TextUtils.isEmpty(name)) {
                        int lastDotIndex = name.lastIndexOf(".");
                        if (lastDotIndex > 0) {
                            String fileSuffix = name.substring(lastDotIndex + 1);
                            fileInfo.setType(fileSuffix);
                        } else if (!TextUtils.isEmpty(mimeType)) {
                            // In some cases displayName doesn't include the file extension; derive
                            // it from mimeType instead
                            String extension =
                                    MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                            if (!TextUtils.isEmpty(extension)) {
                                fileInfo.setName(fileInfo.getName() + "." + extension);
                                fileInfo.setType(extension);
                            }
                        } else {
                            RLog.e(TAG, "getFileInfoByContent mimeType null");
                        }
                    } else {
                        RLog.e(TAG, "getFileInfoByContent getName is empty");
                        return null;
                    }
                    if (cursor.moveToNext()) {
                        RLog.e(TAG, "uri is error,cursor has second value,uri is" + uri);
                    }
                    return fileInfo;
                }
            } catch (Exception e) {
                RLog.e(TAG, "getDocumentByContent is error", e);
                return null;
            } finally {
                if (cursor != null) cursor.close();
            }
        } else {
            RLog.e(TAG, "getDocumentByContent uri is not content");
            return null;
        }
        return null;
    }

    /**
     * Detects the file type from an input stream.
     *
     * @param inputStream input stream
     * @return file type extension
     */
    public static String getFileTypeFromInputStream(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        byte[] b = new byte[4];
        try {
            int result = inputStream.read(b, 0, b.length);
            if (result != b.length) {
                return "";
            }
            String header = bytesToHexString(b);

            String fileType = "";

            if (header != null && header.length() > 0) {
                List<String> lFileTypes = new ArrayList<>(FILE_TYPE_MAP.keySet());

                for (String lFileType : lFileTypes) {
                    if (header.contains(lFileType.toUpperCase())
                            || lFileType.toUpperCase().contains(header)) {
                        fileType = FILE_TYPE_MAP.get(lFileType);
                        break;
                    }
                }
            }
            return fileType;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getFileTypeFromByteData(byte[] data) {
        try {
            if (data == null || data.length <= 4) {
                return null;
            }
            byte[] b = new byte[4];
            System.arraycopy(data, 0, b, 0, b.length);
            String header = bytesToHexString(b);

            String fileType = "";

            if (header != null && header.length() > 0) {
                List<String> lFileTypes = new ArrayList<>(FILE_TYPE_MAP.keySet());

                for (String lFileType : lFileTypes) {
                    if (header.contains(lFileType.toUpperCase())
                            || lFileType.toUpperCase().contains(header)) {
                        fileType = FILE_TYPE_MAP.get(lFileType);
                        break;
                    }
                }
            }
            return fileType;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Converts a byte array of file header info to a hex string representation.
     *
     * @param src byte array of file header
     * @return hex string of file header
     */
    private static String bytesToHexString(byte[] src) {

        StringBuilder builder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        String hv;
        for (byte b : src) {
            // Convert to uppercase hexadecimal (base 16) unsigned integer string representation
            hv = Integer.toHexString(b & 0xFF).toUpperCase();
            if (hv.length() < 2) {
                builder.append(0);
            }
            builder.append(hv);
        }
        return builder.toString();
    }

    public static InputStream getFileInputStream(Context context, Uri uri) {
        try {
            if (FileUtils.uriStartWithFile(uri)) {
                String path = uri.toString().substring(7);
                return new FileInputStream(new File(path));
            } else if (FileUtils.uriStartWithContent(uri)) {
                return context.getContentResolver().openInputStream(uri);
            } else {
                if (uri != null) {
                    return new FileInputStream(new File(uri.toString()));
                }
            }
        } catch (Exception e) {
            RLog.e(TAG, "getFileInputStream error:" + e.getMessage());
        }
        return null;
    }

    /** On versions below 9.0, getExternalCacheDir may throw ArrayIndexOutOfBoundsException */
    public static File getExternalCacheDir(Context context) {
        if (context == null) {
            return null;
        }
        File externalCacheDir = null;
        try {
            externalCacheDir = context.getExternalCacheDir();
        } catch (Exception e) {
        }
        return externalCacheDir;
    }

    public static String generateKey() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        byte[] resBytes = timestamp.getBytes();
        for (int i = 0, len = resBytes.length - INT_4, j = len; i < len; i++, j = len + i % INT_4) {
            resBytes[i] = (byte) (resBytes[i] ^ resBytes[j]);
        }
        String encodeToString = Base64.encodeToString(resBytes, Base64.URL_SAFE);
        return encodeToString.replace("+", "-").replace("/", "-");
    }

    public static String getJsonOutPath(Context context) {
        try {
            String dir =
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
            return dir + "/" + System.currentTimeMillis() + ".json";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Sanitizes the file name to prevent path traversal attacks.
     *
     * @param displayName file name
     * @return sanitized file name
     */
    public static String sanitizeFilename(String displayName) {
        if (displayName == null) {
            return null;
        }
        String[] badCharacters = new String[] {"..", "/"};
        String[] segments = displayName.split("/");
        String fileName = segments[segments.length - 1];
        for (String suspString : badCharacters) {
            fileName = fileName.replace(suspString, "_");
        }
        if (!displayName.equals(fileName)) {
            ai.nexconn.chatui.utils.log.RLog.e(
                    TAG, "sanitizeFilename: " + displayName + " -> " + fileName);
        }
        return fileName;
    }

    public static String getUrlFileName(String url, String fileName) {
        if (url.startsWith("http")) {
            Uri uri = Uri.parse(url);
            if (!TextUtils.isEmpty(uri.getPath())) {
                return uri.getPath().replace("/", "");
            }
        }
        return fileName;
    }
}
