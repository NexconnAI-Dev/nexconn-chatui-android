package ai.nexconn.chatui.utils.permission;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.utils.common.ChatUIUtils;
import ai.nexconn.chatui.utils.constant.AndroidConstant;
import ai.nexconn.chatui.utils.log.RLog;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.core.app.AppOpsManagerCompat;
import androidx.fragment.app.Fragment;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Permission check utility class. */
public class PermissionCheckUtil {
    private static final String TAG = PermissionCheckUtil.class.getSimpleName();
    public static final int REQUEST_CODE_ASK_PERMISSIONS = 100;
    public static final int REQUEST_CODE_LOCATION_SHARE = 101;
    private static IRequestPermissionListListener listener;
    private static final String PROMPT = "prompt";
    private static final String IS_PROMPT = "isPrompt";

    public static boolean requestPermissions(Fragment fragment, String[] permissions) {
        return requestPermissions(fragment, permissions, 0);
    }

    public static boolean requestPermissions(
            final Fragment fragment, String[] permissions, final int requestCode) {
        if (permissions.length == 0) {
            return true;
        }

        final List<String> permissionsNotGranted = new ArrayList<>();
        boolean result = false;

        for (String permission : permissions) {
            //            if ((isFlyme() || Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            //                    && permission.equals(Manifest.permission.RECORD_AUDIO)) {
            //                final SharedPreferences sharedPreferences =
            //                        fragment.getContext().getSharedPreferences(PROMPT,
            // Context.MODE_PRIVATE);
            //                boolean isPrompt = sharedPreferences.getBoolean(IS_PROMPT, true);
            //                if (isPrompt) {
            //                    showPermissionAlert(
            //                            fragment.getContext(),
            //                            fragment.getString(R.string.nc_permission_grant_needed)
            //                                    +
            // fragment.getString(R.string.nc_permission_microphone),
            //                            new DialogInterface.OnClickListener() {
            //                                @Override
            //                                public void onClick(DialogInterface dialog, int which)
            // {
            //                                    if (DialogInterface.BUTTON_POSITIVE == which) {
            //                                        fragment.startActivity(
            //                                                new Intent(
            //                                                        Settings
            //
            // .ACTION_MANAGE_APPLICATIONS_SETTINGS));
            //                                    } else if (DialogInterface.BUTTON_NEUTRAL ==
            // which) {
            //                                        SharedPreferences.Editor editor =
            //                                                sharedPreferences
            //                                                        .edit()
            //                                                        .putBoolean(IS_PROMPT, false);
            //                                        editor.commit();
            //                                    }
            //                                }
            //                            });
            //                }
            //                return false;
            //            }
            if (!hasPermission(fragment.getActivity(), permission)) {
                permissionsNotGranted.add(permission);
            }
        }

        if (permissionsNotGranted.size() > 0) {
            final int size = permissionsNotGranted.size();
            if (listener != null) {
                listener.onRequestPermissionList(
                        fragment.getActivity(),
                        permissionsNotGranted,
                        new IPermissionEventCallback() {
                            @Override
                            public void confirmed() {
                                fragment.requestPermissions(
                                        permissionsNotGranted.toArray(new String[size]),
                                        requestCode);
                            }

                            @Override
                            public void cancelled() {
                                // do nothing
                            }
                        });
            } else {
                fragment.requestPermissions(
                        permissionsNotGranted.toArray(new String[size]), requestCode);
            }
        } else {
            result = true;
        }
        return result;
    }

    public static boolean requestPermissions(
            final Activity activity, @NonNull String[] permissions) {
        return requestPermissions(activity, permissions, 0);
    }

    @TargetApi(23)
    public static boolean requestPermissions(
            final Activity activity, @NonNull final String[] permissions, final int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (permissions.length == 0) {
            return true;
        }

        final List<String> permissionsNotGranted = new ArrayList<>();
        boolean result = false;

        for (String permission : permissions) {
            if (!hasPermission(activity, permission)) {
                permissionsNotGranted.add(permission);
            }
        }

        if (permissionsNotGranted.size() > 0) {
            final int size = permissionsNotGranted.size();
            if (listener != null) {
                listener.onRequestPermissionList(
                        activity,
                        permissionsNotGranted,
                        new IPermissionEventCallback() {
                            @Override
                            public void confirmed() {
                                if (activity != null) {
                                    activity.requestPermissions(
                                            permissionsNotGranted.toArray(new String[size]),
                                            requestCode);
                                }
                            }

                            @Override
                            public void cancelled() {
                                // do nothing
                            }
                        });
            } else {
                if (activity != null) {
                    activity.requestPermissions(
                            permissionsNotGranted.toArray(new String[size]), requestCode);
                }
            }
        } else {
            result = true;
        }
        return result;
    }

    public static boolean checkPermissions(Context context, @NonNull String[] permissions) {
        if (permissions.length == 0) {
            return true;
        }
        for (String permission : permissions) {
            if ((isFlyme() || (Build.VERSION.SDK_INT < Build.VERSION_CODES.M))
                    && permission.equals(Manifest.permission.RECORD_AUDIO)) {
                RLog.i(TAG, "Build.MODEL = " + Build.MODEL);
                if ((Build.BRAND.toLowerCase().equals("meizu"))) {
                    // For Meizu devices, use a dual-check approach
                    if (hasPermission(context, permission) || hasRecordPermision(context)) {
                        continue;
                    } else {
                        return false;
                    }
                }

                if (!hasRecordPermision(context)) {
                    return false;
                } else {
                    continue;
                }
            }
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static String[] getMediaStoragePermissions(Context context) {
        String[] permissions;
        if (ChatUIUtils.checkSDKVersionAndTargetIsUDC(context)) {
            permissions =
                    new String[] {
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    };
        } else if (ChatUIUtils.checkSDKVersionAndTargetIsTIRAMISU(context)) {
            permissions =
                    new String[] {
                        Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO
                    };
        } else {
            permissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        return permissions;
    }

    public static boolean checkMediaStoragePermissions(Context context) {
        // CAMERA permission is requested when the user taps "take photo" after entering the
        // gallery.
        // On Android 14, two permission scenarios allow opening the media library:
        // 1. Full permissions: READ_MEDIA_IMAGES and READ_MEDIA_VIDEO
        // 2. Partial permissions: READ_MEDIA_VISUAL_USER_SELECTED
        // If neither is granted, show a warning dialog.
        if (Build.VERSION.SDK_INT >= AndroidConstant.ANDROID_UPSIDE_DOWN_CAKE) {
            String[] allPermissions =
                    new String[] {
                        Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO
                    };
            String[] subPermissions =
                    new String[] {Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED};

            if (checkPermissions(context, allPermissions)) {
                return true;
            }
            return checkPermissions(context, subPermissions);
        }
        String[] permissions = getMediaStoragePermissions(context);
        return checkPermissions(context, permissions);
    }

    private static boolean isFlyme() {
        String osString = "";
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method get = clz.getMethod("get", String.class, String.class);
            osString = (String) get.invoke(clz, "ro.build.display.id", "");
        } catch (Exception e) {
            RLog.e(TAG, "isFlyme", e);
        }
        return osString != null && osString.toLowerCase().contains("flyme");
    }

    private static boolean hasRecordPermision(Context context) {
        boolean hasPermission = false;
        int bufferSizeInBytes =
                AudioRecord.getMinBufferSize(
                        44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSizeInBytes < 0) {
            RLog.e(TAG, "bufferSizeInBytes = " + bufferSizeInBytes);
            return false;
        }
        AudioRecord audioRecord;
        try {
            audioRecord =
                    new AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            44100,
                            AudioFormat.CHANNEL_IN_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSizeInBytes);
            audioRecord.startRecording();
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                hasPermission = true;
                audioRecord.stop();
            }
            audioRecord.release();
        } catch (Exception e) {
            RLog.e(TAG, "Audio record exception.");
        }
        return hasPermission;
    }

    // Fix duplicate permission names in the permission prompt dialog
    private static String getNotGrantedPermissionMsg(
            Context context, String[] permissions, int[] grantResults) {
        if (checkPermissionResultIncompatible(permissions, grantResults)) {
            return "";
        }

        List<String> permissionNameList = new ArrayList<>(permissions.length);
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                String permissionName = resolvePermissionDisplayName(context, permissions[i]);
                if (!TextUtils.isEmpty(permissionName)
                        && !permissionNameList.contains(permissionName)) {
                    permissionNameList.add(permissionName);
                }
            }
        }

        StringBuilder builder =
                new StringBuilder(
                        context.getResources().getString(R.string.nc_permission_grant_needed));
        if (permissionNameList.isEmpty()) {
            return builder.toString();
        }
        return builder.append(" (")
                .append(TextUtils.join(" ", permissionNameList))
                .append(")")
                .toString();
    }

    private static String getNotGrantedPermissionMsg(Context context, List<String> permissions) {
        if (permissions == null || permissions.size() == 0) {
            return "";
        }
        Set<String> permissionsValue = new HashSet<>();
        for (String permission : permissions) {
            String permissionValue = resolvePermissionDisplayName(context, permission);
            if (!TextUtils.isEmpty(permissionValue)) {
                permissionsValue.add(permissionValue);
            }
        }
        if (permissionsValue.isEmpty()) {
            return "";
        }
        return "(" + TextUtils.join(" ", permissionsValue) + ")";
    }

    private static String resolvePermissionDisplayName(Context context, String permission) {
        if (context == null || TextUtils.isEmpty(permission)) {
            return "";
        }
        int resId =
                context.getResources()
                        .getIdentifier("nc_" + permission, "string", context.getPackageName());
        if (resId != 0) {
            try {
                return context.getString(resId);
            } catch (Resources.NotFoundException e) {
                RLog.e(TAG, "Permission resource not found for: " + permission);
            }
        }

        switch (permission) {
            case Manifest.permission.CAMERA:
                return context.getString(R.string.nc_permission_name_camera);
            case Manifest.permission.RECORD_AUDIO:
                return context.getString(R.string.nc_permission_name_microphone);
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return context.getString(R.string.nc_permission_name_storage);
            case Manifest.permission.READ_MEDIA_IMAGES:
                return context.getString(R.string.nc_permission_name_photos);
            case Manifest.permission.READ_MEDIA_VIDEO:
                return context.getString(R.string.nc_permission_name_videos);
            case Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED:
                return context.getString(R.string.nc_permission_name_partial_photos);
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
            case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                return context.getString(R.string.nc_permission_name_location);
            case Manifest.permission.BLUETOOTH_CONNECT:
                return context.getString(R.string.nc_permission_name_bluetooth);
            case Manifest.permission.READ_PHONE_STATE:
            case Manifest.permission.PROCESS_OUTGOING_CALLS:
                return context.getString(R.string.nc_permission_name_phone);
            case Settings.ACTION_MANAGE_OVERLAY_PERMISSION:
                return context.getString(R.string.nc_permission_name_overlay);
            default:
                return permission;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void showPermissionAlert(
            Context context, String content, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setMessage(content)
                .setPositiveButton(R.string.nc_confirm, listener)
                .setNegativeButton(R.string.nc_cancel, listener)
                .setNeutralButton(R.string.nc_not_prompt, listener)
                .setCancelable(false)
                .create()
                .show();
    }

    @TargetApi(19)
    public static boolean canDrawOverlays(Context context) {
        return canDrawOverlays(context, true);
    }

    /**
     * Checks whether the app has overlay (floating window) permission.
     *
     * @param context context
     * @return whether the permission is granted
     */
    @TargetApi(19)
    public static boolean canDrawOverlays(
            final Context context, boolean needOpenPermissionSetting) {
        boolean result = true;
        boolean booleanValue;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                booleanValue =
                        (Boolean)
                                Settings.class
                                        .getDeclaredMethod("canDrawOverlays", Context.class)
                                        .invoke(null, new Object[] {context});
                if (!booleanValue && needOpenPermissionSetting) {
                    ArrayList<String> permissionList = new ArrayList<>();
                    permissionList.add(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    showPermissionAlert(
                            context,
                            context.getString(R.string.nc_permission_grant_needed)
                                    + getNotGrantedPermissionMsg(context, permissionList),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (DialogInterface.BUTTON_POSITIVE == which) {
                                        Intent intent =
                                                new Intent(
                                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                        Uri.parse(
                                                                "package:"
                                                                        + context
                                                                                .getPackageName()));
                                        context.startActivity(intent);
                                    }
                                }
                            });
                }
                RLog.i(TAG, "isFloatWindowOpAllowed allowed: " + booleanValue);
                return booleanValue;
            } catch (Exception e) {
                RLog.e(
                        TAG,
                        String.format(
                                "getDeclaredMethod:canDrawOverlays! Error:%s, etype:%s",
                                e.getMessage(), e.getClass().getCanonicalName()));
                return true;
            }
        } else if (Build.VERSION.SDK_INT < 19) {
            return true;
        } else {
            Method method;
            Object systemService = context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                method =
                        Class.forName("android.app.AppOpsManager")
                                .getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
            } catch (NoSuchMethodException e) {
                RLog.e(
                        TAG,
                        String.format(
                                "NoSuchMethodException method:checkOp! Error:%s", e.getMessage()));
                method = null;
            } catch (ClassNotFoundException e) {
                RLog.e(TAG, "canDrawOverlays", e);
                method = null;
            }
            if (method != null) {
                try {
                    Integer tmp =
                            (Integer)
                                    method.invoke(
                                            systemService,
                                            new Object[] {
                                                24,
                                                context.getApplicationInfo().uid,
                                                context.getPackageName()
                                            });
                    result = tmp != null && tmp == 0;
                } catch (Exception e) {
                    RLog.e(
                            TAG,
                            String.format(
                                    "call checkOp failed: %s etype:%s",
                                    e.getMessage(), e.getClass().getCanonicalName()));
                }
            }
            RLog.i(TAG, "isFloatWindowOpAllowed allowed: " + result);
            return result;
        }
    }

    private static boolean hasPermission(Context context, String permission) {
        String opStr = AppOpsManagerCompat.permissionToOp(permission);
        if (opStr == null && Build.VERSION.SDK_INT < 23) {
            return true;
        }
        return context != null
                && context.checkCallingOrSelfPermission(permission)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public static void showRequestPermissionFailedAlter(
            final Context context, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (context == null) {
            return;
        }
        String content = getNotGrantedPermissionMsg(context, permissions, grantResults);
        if (TextUtils.isEmpty(content)) {
            return;
        }
        DialogInterface.OnClickListener listener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                Intent intent =
                                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                                intent.setData(uri);
                                context.startActivity(intent);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                            default:
                                break;
                        }
                    }
                };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert)
                    .setMessage(content)
                    .setPositiveButton(R.string.nc_confirm, listener)
                    .setNegativeButton(R.string.nc_cancel, listener)
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            new AlertDialog.Builder(context)
                    .setMessage(content)
                    .setPositiveButton(R.string.nc_confirm, listener)
                    .setNegativeButton(R.string.nc_cancel, listener)
                    .setCancelable(false)
                    .create()
                    .show();
        }
    }

    /**
     * Validates that permission callback parameters are compatible.
     *
     * @param grantResults authorization results returned by the system
     * @return {@code true} if parameters are incompatible; {@code false} if they match
     */
    public static boolean checkPermissionResultIncompatible(
            String[] permissions, int[] grantResults) {
        return grantResults == null
                || grantResults.length == 0
                || permissions == null
                || permissions.length != grantResults.length;
    }

    /**
     * Sets the permission request interceptor listener.
     *
     * @param listener the listener
     */
    public static void setRequestPermissionListListener(IRequestPermissionListListener listener) {
        if (listener == null) {
            return;
        }
        PermissionCheckUtil.listener = listener;
    }

    /**
     * Listener that allows the app to intercept SDK permission requests. Implement {@code
     * onRequestPermissionList} to show a dialog explaining why the permissions are needed.
     */
    public interface IRequestPermissionListListener {
        /**
         * @param activity
         * @param permissionsNotGranted
         * @param callback callback for the dialog's confirm and cancel button events
         */
        void onRequestPermissionList(
                Context activity,
                List<String> permissionsNotGranted,
                IPermissionEventCallback callback);
    }

    /**
     * Callback interface for the permission rationale dialog's confirm and cancel button events.
     */
    public interface IPermissionEventCallback {
        void confirmed();

        void cancelled();
    }
}
