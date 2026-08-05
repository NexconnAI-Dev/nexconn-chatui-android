package ai.nexconn.chatui.utils.language;

import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.system.SystemUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.view.ContextThemeWrapper;
import java.util.Locale;

/** Configuration manager for locale and file size settings. */
public class NCConfigurationManager {
    private static final String TAG = NCConfigurationManager.class.getSimpleName();
    private static String NC_CONFIG = "NCKitConfiguration";
    private static String FILE_MAX_SIZE = "FileMaxSize";
    private static boolean isInit = false;

    private NCConfigurationManager() {
        // default implementation ignored
    }

    private static class SingletonHolder {
        static NCConfigurationManager sInstance = new NCConfigurationManager();
    }

    /**
     * Listens for system locale changes to prevent the app language from switching unexpectedly.
     */
    private static class SystemConfigurationChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
                LangUtils.setSystemLocale(Locale.getDefault());
                LangUtils.NCLocale appLocale = LangUtils.getAppLocale(context);
                Locale systemLocale = LangUtils.getSystemLocale();
                if (!appLocale.toLocale().equals(systemLocale)) {
                    NCConfigurationManager.getInstance().switchLocale(appLocale, context);
                }
            }
        }
    }

    public static NCConfigurationManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public static void init(Context context) {
        if (!isInit) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            SystemUtils.registerReceiverCompat(
                    context, new SystemConfigurationChangedReceiver(), filter);

            // Restore the previously saved app language during initialization
            LangUtils.NCLocale locale = NCConfigurationManager.getInstance().getAppLocale(context);
            NCConfigurationManager.getInstance().switchLocale(locale, context);
            isInit = true;
        }
    }

    /**
     * Sets the maximum file size for sending files.
     *
     * @param context context
     * @param size maximum file size in MB
     */
    public void setFileMaxSize(Context context, int size) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(NC_CONFIG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(FILE_MAX_SIZE, size).apply();
    }

    /**
     * Gets the maximum file size for sending files.
     *
     * @param context context
     * @return maximum file size in MB
     */
    public int getFileMaxSize(Context context) {
        if (context == null) {
            return 100;
        }
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(NC_CONFIG, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(FILE_MAX_SIZE, 100);
    }

    /**
     * Switches the app language.
     *
     * @param locale accepted values: NCLocale.LOCALE_CHINA, NCLocale.LOCALE_US, or NCLocale.AUTO
     */
    public void switchLocale(LangUtils.NCLocale locale, Context context) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale.toLocale();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            context.getResources().updateConfiguration(config, resources.getDisplayMetrics());
        }
        LangUtils.saveLocale(context, locale);
    }

    /**
     * Creates a ConfigurationContext for use in attachBaseContext.
     *
     * @param newBase the base context
     * @return the configuration-wrapped context
     */
    public Context getConfigurationContext(Context newBase) {
        Context context = LangUtils.getConfigurationContext(newBase);
        final Configuration configuration = context.getResources().getConfiguration();
        try {
            return new ContextThemeWrapper(
                    context, androidx.appcompat.R.style.Theme_AppCompat_Empty) {
                @Override
                public void applyOverrideConfiguration(Configuration overrideConfiguration) {
                    if (overrideConfiguration != null) {
                        overrideConfiguration.setTo(configuration);
                    }
                    super.applyOverrideConfiguration(overrideConfiguration);
                }
            };
        } catch (Exception e) {
            RLog.e(TAG, "getConfigurationContext e : ", e);
            return context;
        }
    }

    /**
     * Gets the in-app locale setting.
     *
     * @return the in-app locale
     */
    public LangUtils.NCLocale getAppLocale(Context context) {
        return LangUtils.getAppLocale(context);
    }

    /**
     * Gets the system locale.
     *
     * @return the system locale
     */
    public Locale getSystemLocale() {
        return LangUtils.getSystemLocale();
    }

    /**
     * Gets the current app language setting.
     *
     * @return the resolved locale
     */
    public LangUtils.NCLocale getLanguageLocal(Context context) {
        LangUtils.NCLocale appLocale = NCConfigurationManager.getInstance().getAppLocale(context);
        if (appLocale == LangUtils.NCLocale.LOCALE_AUTO) {
            Locale systemLocale = NCConfigurationManager.getInstance().getSystemLocale();
            if (systemLocale.getLanguage().equals(Locale.CHINESE.getLanguage())) {
                appLocale = LangUtils.NCLocale.LOCALE_CHINA;
            } else if (systemLocale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                appLocale = LangUtils.NCLocale.LOCALE_US;
            } else if (systemLocale.getLanguage().equals(new Locale("ar").getLanguage())) {
                appLocale = LangUtils.NCLocale.LOCALE_ARAB;
            } else {
                appLocale = LangUtils.NCLocale.LOCALE_US;
            }
        }
        return appLocale;
    }
}
