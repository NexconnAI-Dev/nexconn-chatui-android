package ai.nexconn.chatui.utils.language;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import java.util.Locale;

/** Language utility class for locale management. */
public class LangUtils {
    private static final String LOCALE_CONF_FILE_NAME = "locale.config";
    private static final String APP_LOCALE = "app_locale";
    private static final String APP_PUSH_LANGUAGE = "app_push_language";
    private static Locale systemLocale = Locale.getDefault();

    public static Context getConfigurationContext(Context context) {
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        Context configurationContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = new LocaleList(getAppLocale(context).toLocale());
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
            configurationContext = context.createConfigurationContext(config);
        }
        return configurationContext;
    }

    public static NCLocale getAppLocale(Context context) {
        SharedPreferences sp =
                context.getSharedPreferences(LOCALE_CONF_FILE_NAME, Context.MODE_PRIVATE);
        String locale = sp.getString(APP_LOCALE, NCLocale.LOCALE_US.value());
        // Migrate legacy "auto" default to English to keep first-run behavior deterministic.
        if (NCLocale.LOCALE_AUTO.value().equals(locale)) {
            return NCLocale.LOCALE_US;
        }
        return NCLocale.valueOf(locale);
    }

    public static void saveLocale(Context context, NCLocale locale) {
        SharedPreferences sp =
                context.getSharedPreferences(LOCALE_CONF_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(APP_LOCALE, locale.value()).commit();
    }

    /** Wrapper class for selectable locales. */
    public static class NCLocale {
        /** Chinese */
        public static final NCLocale LOCALE_CHINA = new NCLocale("zh");

        /** English */
        public static final NCLocale LOCALE_US = new NCLocale("en");

        /** Arabic */
        public static final NCLocale LOCALE_ARAB = new NCLocale("ar");

        /** Follow system locale */
        public static final NCLocale LOCALE_AUTO = new NCLocale("auto");

        private String NCLocale;

        private NCLocale(String NCLocale) {
            this.NCLocale = NCLocale;
        }

        public String value() {
            return NCLocale;
        }

        public Locale toLocale() {
            Locale locale;
            if (NCLocale.equals(LOCALE_CHINA.value())) {
                locale = Locale.CHINESE;
            } else if (NCLocale.equals(LOCALE_US.value())) {
                locale = Locale.ENGLISH;
            } else if (NCLocale.equals(LOCALE_ARAB.value())) {
                locale = new Locale("ar");
            } else {
                locale = getSystemLocale();
            }
            return locale;
        }

        public static NCLocale valueOf(String NCLocale) {
            NCLocale locale;
            if (LOCALE_CHINA.value().equals(NCLocale)) {
                locale = LOCALE_CHINA;
            } else if (LOCALE_US.value().equals(NCLocale)) {
                locale = LOCALE_US;
            } else if (LOCALE_ARAB.value().equals(NCLocale)) {
                locale = LOCALE_ARAB;
            } else {
                locale = LOCALE_AUTO;
            }
            return locale;
        }
    }

    /**
     * Gets the system locale.
     *
     * @return the system locale
     */
    public static Locale getSystemLocale() {
        return systemLocale;
    }

    /**
     * Sets the system locale.
     *
     * @param locale the locale to set
     */
    public static void setSystemLocale(Locale locale) {
        systemLocale = locale;
    }

    /**
     * Gets the current language regardless of whether it is explicitly set or following the system.
     *
     * @param context context
     * @return current language
     */
    public static NCLocale getCurrentLanguage(Context context) {
        SharedPreferences sp =
                context.getSharedPreferences(LOCALE_CONF_FILE_NAME, Context.MODE_PRIVATE);
        String locale = sp.getString(APP_LOCALE, NCLocale.LOCALE_US.value());
        if (NCLocale.LOCALE_AUTO.value().equals(locale)) {
            return NCLocale.LOCALE_US;
        }
        return NCLocale.valueOf(locale);
    }

    /**
     * Gets the current app language setting.
     *
     * @return the resolved locale
     */
    public static LangUtils.NCLocale getAppLanguageLocal(Context context) {
        LangUtils.NCLocale appLocale = getAppLocale(context);
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
