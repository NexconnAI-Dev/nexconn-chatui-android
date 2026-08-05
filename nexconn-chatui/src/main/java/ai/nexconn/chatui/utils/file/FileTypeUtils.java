package ai.nexconn.chatui.utils.file;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.FileInfo;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** File type utility class for identifying and opening files by type. */
public class FileTypeUtils {

    private static final String TAG = FileTypeUtils.class.getSimpleName();

    public static int fileTypeImageId(Context context, String fileName) {
        int id = serchFileTypeInRegister(fileName);
        if (id > 0) {
            return id;
        }
        if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_image_file_suffix)))
            id = R.drawable.nc_lively_file_picture;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_file_file_suffix)))
            id = R.drawable.nc_lively_file_file;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_video_file_suffix)))
            id = R.drawable.nc_lively_file_video;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_audio_file_suffix)))
            id = R.drawable.nc_lively_file_audio;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_word_file_suffix)))
            id = R.drawable.nc_lively_file_word;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_excel_file_suffix)))
            id = R.drawable.nc_lively_file_excel;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_ppt_file_suffix)))
            id = R.drawable.nc_lively_file_ppt;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_pdf_file_suffix)))
            id = R.drawable.nc_lively_file_pdf;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_apk_file_suffix)))
            id = R.drawable.nc_lively_file_apk;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_key_file_suffix)))
            id = R.drawable.nc_lively_file_key;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_numbers_file_suffix)))
            id = R.drawable.nc_lively_file_numbers;
        else if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_pages_file_suffix)))
            id = R.drawable.nc_lively_file_pages;
        else {
            id = serchDefaultIconInRegister();
            if (id <= 0) {
                id = R.drawable.nc_lively_file_else;
            }
        }
        return id;
    }

    private static int serchFileTypeInRegister(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return 0;
        }
        String suffix =
                fileName.toLowerCase().substring(fileName.lastIndexOf(".") + 1, fileName.length());
        Integer id = NCChatUIConfig.channelConfig().getFileSuffixTypes().get(suffix);
        return id == null ? 0 : id.intValue();
    }

    private static int serchDefaultIconInRegister() {
        Integer id = NCChatUIConfig.channelConfig().getFileSuffixTypes().get("default");
        return id == null ? 0 : id.intValue();
    }

    private static boolean checkSuffix(String fileName, String[] fileSuffix) {
        for (String suffix : fileSuffix) {
            if (fileName != null) {
                if (fileName.toLowerCase().endsWith(suffix)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Intent getOpenFileIntent(Context context, String fileName, String fileSavePath) {
        Intent intent = new Intent("android.intent.action.VIEW");
        String type = getIntentType(context, intent, fileName);
        if (type != null && fileSavePath != null && isIntentHandlerAvailable(context, intent)) {
            Uri uri =
                    FileProvider.getUriForFile(
                            context,
                            context.getApplicationContext().getPackageName()
                                    + context.getResources()
                                            .getString(R.string.nc_authorities_fileprovider),
                            new File(fileSavePath));
            intent.setDataAndType(uri, type);
            return intent;
        } else {
            return null;
        }
    }

    public static Intent getOpenFileIntent(Context context, String fileName, Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        String type = getIntentType(context, intent, fileName);
        if (type != null && uri != null && isIntentHandlerAvailable(context, intent)) {
            if (FileUtils.uriStartWithContent(uri)) {
                intent.setDataAndType(uri, type);
            } else {
                // Starts with "file://"
                String path = uri.toString();
                if (FileUtils.uriStartWithFile(uri)) {
                    path = path.substring(7);
                }
                Uri fileUri =
                        FileProvider.getUriForFile(
                                context,
                                context.getApplicationContext().getPackageName()
                                        + context.getResources()
                                                .getString(R.string.nc_authorities_fileprovider),
                                new File(path));
                intent.setDataAndType(fileUri, type);
            }
            return intent;
        } else {
            return null;
        }
    }

    private static String getIntentType(Context context, Intent intent, String fileName) {
        String type = null;
        if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_image_file_suffix))) {
            intent.addCategory("android.intent.category.DEFAULT");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            type = "image/*";
        }
        if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_file_file_suffix))) {
            intent.addCategory("android.intent.category.DEFAULT");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            type = "text/plain";
        }
        if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_video_file_suffix))) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("oneshot", 0);
            intent.putExtra("configchange", 0);
            type = "video/*";
        }
        if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_audio_file_suffix))) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("oneshot", 0);
            intent.putExtra("configchange", 0);
            type = "audio/*";
        }
        if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_word_file_suffix))) {
            intent.addCategory("android.intent.category.DEFAULT");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            type = "application/msword";
        }
        if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_excel_file_suffix))) {
            intent.addCategory("android.intent.category.DEFAULT");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            type = "application/vnd.ms-excel";
        }
        if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_pdf_file_suffix))) {
            intent.addCategory("android.intent.category.DEFAULT");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            type = "application/pdf";
        }

        if (checkSuffix(
                fileName, context.getResources().getStringArray(R.array.nc_ppt_file_suffix))) {
            intent.addCategory("android.intent.category.DEFAULT");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            type = "application/vnd.ms-powerpoint";
        }
        return type;
    }

    private static boolean isIntentHandlerAvailable(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> infoList =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return infoList.size() > 0;
    }

    /** File filter that excludes hidden files */
    public static final FileFilter ALL_FOLDER_AND_FILES_FILTER =
            new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return !pathname.isHidden();
                }
            };

    public static final class FileTypeFilter implements FileFilter {

        private String[] filesSuffix;

        public FileTypeFilter(String[] fileSuffix) {
            this.filesSuffix = fileSuffix;
        }

        @Override
        public boolean accept(File pathname) {
            return !pathname.isHidden()
                    && (pathname.isDirectory() || checkSuffix(pathname.getName(), filesSuffix));
        }
    }

    public static List<FileInfo> getTextFilesInfo(Context context, File fileDir) {
        List<FileInfo> textFilesInfo = new ArrayList<>();
        FileFilter fileFilter =
                new FileTypeFilter(
                        context.getResources().getStringArray(R.array.nc_file_file_suffix));
        getFileInfos(fileDir, fileFilter, textFilesInfo);
        return textFilesInfo;
    }

    private static void getFileInfos(
            File fileDir, FileFilter fileFilter, List<FileInfo> fileInfos) {
        File[] listFiles = fileDir.listFiles(fileFilter);
        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.isDirectory()) {
                    getFileInfos(file, fileFilter, fileInfos);
                } else {
                    if (file.length() == 0) {
                        continue;
                    }
                    FileInfo fileInfo = getFileInfoFromFile(file);
                    fileInfos.add(fileInfo);
                }
            }
        }
    }

    public static List<FileInfo> getVideoFilesInfo(Context context, File fileDir) {
        List<FileInfo> videoFilesInfo = new ArrayList<>();
        FileFilter fileFilter =
                new FileTypeFilter(
                        context.getResources().getStringArray(R.array.nc_video_file_suffix));
        getFileInfos(fileDir, fileFilter, videoFilesInfo);
        return videoFilesInfo;
    }

    public static List<FileInfo> getAudioFilesInfo(Context context, File fileDir) {
        List<FileInfo> audioFilesInfo = new ArrayList<>();
        FileFilter fileFilter =
                new FileTypeFilter(
                        context.getResources().getStringArray(R.array.nc_audio_file_suffix));
        getFileInfos(fileDir, fileFilter, audioFilesInfo);
        return audioFilesInfo;
    }

    public static List<FileInfo> getOtherFilesInfo(Context context, File fileDir) {
        List<FileInfo> otherFilesInfo = new ArrayList<>();
        FileFilter fileFilter =
                new FileTypeFilter(
                        context.getResources().getStringArray(R.array.nc_other_file_suffix));
        getFileInfos(fileDir, fileFilter, otherFilesInfo);
        return otherFilesInfo;
    }

    public static List<FileInfo> getFileInfosFromFileArray(File[] files) {
        List<FileInfo> fileInfos = new ArrayList<>();
        for (File file : files) {
            FileInfo fileInfo = getFileInfoFromFile(file);
            fileInfos.add(fileInfo);
        }
        return fileInfos;
    }

    private static FileInfo getFileInfoFromFile(final File file) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(file.getName());
        fileInfo.setFilePath(file.getPath());
        fileInfo.setDirectory(file.isDirectory());
        if (file.isDirectory()) {
            fileInfo.setFileSize(FileTypeUtils.getNumFilesInFolder(fileInfo));
        } else {
            fileInfo.setFileSize(file.length());
        }
        int lastDotIndex = file.getName().lastIndexOf(".");
        if (lastDotIndex > 0) {
            String fileSuffix = file.getName().substring(lastDotIndex + 1);
            fileInfo.setSuffix(fileSuffix);
        }
        return fileInfo;
    }

    /** Comparator that sorts files by name */
    public static class FileNameComparator implements Comparator<FileInfo> {
        protected static final int FIRST = -1, SECOND = 1;

        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            if (lhs.isDirectory() || rhs.isDirectory()) {
                if (lhs.isDirectory() == rhs.isDirectory())
                    return lhs.getFileName().compareToIgnoreCase(rhs.getFileName());
                else if (lhs.isDirectory()) return FIRST;
                else return SECOND;
            }
            return lhs.getFileName().compareToIgnoreCase(rhs.getFileName());
        }
    }

    /**
     * Gets the number of files in a folder.
     *
     * @param fileInfo file info
     * @return number of files in the folder
     */
    public static int getNumFilesInFolder(FileInfo fileInfo) {
        if (!fileInfo.isDirectory()) return 0;
        File[] files = new File(fileInfo.getFilePath()).listFiles(ALL_FOLDER_AND_FILES_FILTER);
        if (files == null) return 0;
        return files.length;
    }

    public static final int KILOBYTE = 1024, MEGABYTE = KILOBYTE * 1024, GIGABYTE = MEGABYTE * 1024;

    /** Converts file size to a human-readable string */
    public static String formatFileSize(long size) {
        if (size < KILOBYTE) {
            return String.format("%d B", (int) size);
        } else if (size < MEGABYTE) return String.format("%.2f KB", (float) size / KILOBYTE);
        else if (size < GIGABYTE) return String.format("%.2f MB", (float) size / MEGABYTE);
        else return String.format("%.2f G", (float) size / GIGABYTE);
    }

    /* returns external storage paths (directory of external memory card) as array of Strings */
    public static String[] getExternalStorageDirectories(Context context) {

        List<String> results = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // Method 1 for KitKat & above
            File[] externalDirs = context.getExternalFilesDirs(null);

            for (File file : externalDirs) {
                if (file != null) {
                    String path = file.getPath().split("/Android")[0];

                    boolean addPath;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        addPath = Environment.isExternalStorageRemovable(file);
                    } else {
                        addPath =
                                Environment.MEDIA_MOUNTED.equals(
                                        EnvironmentCompat.getStorageState(file));
                    }

                    if (addPath) {
                        results.add(path);
                    }
                }
            }
        }

        if (results.isEmpty()) { // Method 2 for all versions
            // better variation of: http://stackoverflow.com/a/40123073/5002496
            String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
            StringBuilder s = new StringBuilder();
            InputStream is = null;
            try {
                final Process process =
                        new ProcessBuilder().command("mount").redirectErrorStream(true).start();
                process.waitFor();
                is = process.getInputStream();
                final byte[] buffer = new byte[1024];
                while (is.read(buffer) != -1) {
                    s.append(new String(buffer));
                }
            } catch (final IOException e) {
                RLog.e(TAG, "getExternalStorageDirectories IOException ", e);
            } catch (InterruptedException e) {
                RLog.e(TAG, "getExternalStorageDirectories InterruptedException ", e);
                Thread.currentThread().interrupt();
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    RLog.e(TAG, "getExternalStorageDirectories IOException close e");
                }
            }

            // parse output
            final String[] lines = s.toString().split("\n");
            for (String line : lines) {
                if (!line.toLowerCase(Locale.US).contains("asec")) {
                    if (line.matches(reg)) {
                        String[] parts = line.split(" ");
                        for (String part : parts) {
                            if (part.startsWith("/"))
                                if (!part.toLowerCase(Locale.US).contains("vold"))
                                    results.add(part);
                        }
                    }
                }
            }
        }

        // Below few lines is to remove paths which may not be external memory card, like OTG (feel
        // free to comment them out)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).toLowerCase().matches(".*[0-9a-f]{4}[-][0-9a-f]{4}")) {
                    results.remove(i--);
                }
            }
        } else {
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).toLowerCase().contains("ext")
                        && !results.get(i).toLowerCase().contains("sdcard")) {
                    results.remove(i--);
                }
            }
        }

        String[] storageDirectories = new String[results.size()];
        for (int i = 0; i < results.size(); ++i) storageDirectories[i] = results.get(i);

        return storageDirectories;
    }
}
