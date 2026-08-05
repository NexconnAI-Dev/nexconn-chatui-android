package ai.nexconn.chatui.config;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.utils.log.RLog;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Theme manager for the ChatUI SDK.
 *
 * <p>Manages theme switching and resource resolution for the ChatUI components, supporting multiple
 * themes and automatic light/dark mode adaptation.
 *
 * <h3>Core features</h3>
 *
 * <ul>
 *   <li><b>Built-in theme:</b> provides the lively theme ({@link #LIVELY_THEME}), which
 *       automatically follows the system light/dark mode
 *   <li><b>Custom themes:</b> developers can register and extend custom themes with light/dark
 *       variants and theme stacking
 *   <li><b>System follow:</b> automatically switches with the system light/dark mode
 *   <li><b>Resource resolution:</b> resolves theme attribute IDs and color values based on the
 *       current theme and dark-mode state
 *   <li><b>Auto-apply:</b> applies themes to all Activities via {@link
 *       android.app.Application.ActivityLifecycleCallbacks}
 *   <li><b>Theme listeners:</b> notifies registered {@link OnThemeListener}s on every switch
 * </ul>
 *
 * <h3>Dark-mode note</h3>
 *
 * <p>On some devices, toggling the system dark mode while the app is in the background may not take
 * effect immediately because the system still reports the old value until the app restarts.
 *
 * <p><b>Recommended solution:</b> If your app uses the AppCompat library, call {@code
 * AppCompatDelegate.setDefaultNightMode(nightMode)} in {@code Application.onCreate()} to ensure
 * timely mode switching:
 *
 * <pre>
 * // Follow system dark mode
 * AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
 *
 * // Force light mode
 * AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
 *
 * // Force dark mode
 * AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
 * </pre>
 *
 * <h3>Quick start</h3>
 *
 * <pre>
 * // 1. Switch to the lively theme (auto light/dark)
 * ChatUIThemeManager.changeInnerTheme(context, ChatUIThemeManager.LIVELY_THEME);
 *
 * // 2. Register a custom theme with light/dark variants
 * ChatUIThemeManager.addTheme("CUSTOM_THEME",
 *     R.style.MyCustomLightTheme,
 *     R.style.MyCustomDarkTheme
 * );
 *
 * // 3. Switch to the custom theme, stacked on top of the lively theme
 * ChatUIThemeManager.changeCustomTheme(context, "CUSTOM_THEME", ChatUIThemeManager.LIVELY_THEME);
 *
 * // 4. Resolve a theme attribute to a resource ID at runtime
 * int bgResId = ChatUIThemeManager.getAttrResId(context, R.attr.nc_conversation_bg);
 * view.setBackgroundResource(bgResId);
 *
 * // 5. Query the current theme
 * String currentTheme = ChatUIThemeManager.getCurrentThemeName();
 * </pre>
 */
public class ChatUIThemeManager {

    // ========== Theme change listener ==========

    /**
     * Listener for theme-change events.
     *
     * <p>Registered via {@link ChatUIThemeManager#addThemeListener}; invoked whenever the active
     * theme changes.
     *
     * <pre>
     * OnThemeListener listener = new OnThemeListener() {
     *     {@literal @}Override
     *     public void onThemeChanged(Context context, String oldTheme, String newTheme) {
     *         if (context instanceof Activity) {
     *             ((Activity) context).recreate();
     *         }
     *     }
     * };
     *
     * ChatUIThemeManager.addThemeListener(listener);
     *
     * // Remove when no longer needed to avoid memory leaks
     * ChatUIThemeManager.removeThemeListener(listener);
     * </pre>
     */
    public interface OnThemeListener {
        /**
         * Called when the active theme changes.
         *
         * @param context the context passed to the theme-change call
         * @param oldTheme identifier of the previous theme (e.g. {@link #LIVELY_THEME})
         * @param newTheme identifier of the new theme
         */
        void onThemeChanged(Context context, String oldTheme, String newTheme);
    }

    // ========== Built-in theme identifiers ==========

    /** Lively theme — modern UI that automatically follows the system light/dark mode. */
    public static final String LIVELY_THEME = "LIVELY_THEME";

    // ========== Constants ==========

    private static final String TAG = "ThemeManager";

    // ========== State ==========

    /** Guard flag ensuring the theme system is initialized exactly once. */
    private static volatile boolean isInit = false;

    /**
     * Map from theme identifier to its ordered list of {@link ThemeConfig}s (supports stacking).
     */
    private static final Map<String, List<ThemeConfig>> themeConfigMap = new HashMap<>();

    /** The currently active theme identifier; defaults to {@link #LIVELY_THEME}. */
    private static String currentTheme = LIVELY_THEME;

    /**
     * Base theme for the current custom theme (used for stacking); {@code null} for built-in
     * themes.
     */
    private static String currentBaseTheme = null;

    /** Thread-safe list of registered theme-change listeners. */
    private static final CopyOnWriteArrayList<OnThemeListener> themeListeners =
            new CopyOnWriteArrayList<>();

    // ========== Inner class: theme config ==========

    /** Holds the light and dark style resource IDs for one theme layer. */
    private static class ThemeConfig {
        final int lightStyleResId; // style for light mode
        final int darkStyleResId; // style for dark mode

        ThemeConfig(int lightStyleResId, int darkStyleResId) {
            this.lightStyleResId = lightStyleResId;
            this.darkStyleResId = darkStyleResId;
        }
    }

    // ========== Constructor ==========

    /** Utility class — not instantiable. */
    private ChatUIThemeManager() {
        throw new UnsupportedOperationException("ChatUIThemeManager is a utility class");
    }

    // ========== Initialization ==========

    /**
     * Initializes the theme system (package-private, thread-safe, executed at most once).
     *
     * @param context context
     */
    static void initThemes(Context context) {
        if (context == null) {
            RLog.w(TAG, "initThemes: context is null");
            return;
        }

        if (!isInit) {
            isInit = true;
            try {
                // Register the built-in lively theme (auto light/dark)
                addTheme(LIVELY_THEME, R.style.NCLivelyLightTheme, R.style.NCLivelyDarkTheme);

                // Initialize the theme manager
                initializeThemeManager(context);

                RLog.i(TAG, "Theme initialization completed");
            } catch (Exception e) {
                RLog.e(TAG, "Failed to initialize themes", e);
            }
        }
    }

    // ========== Theme management ==========

    /**
     * Registers a theme with light and dark style variants.
     *
     * <p>Calling this method multiple times for the same {@code themeType} stacks the styles: each
     * additional call appends a new layer, and later layers override earlier ones for the same
     * attribute.
     *
     * <pre>
     * // Register a custom theme with light/dark variants
     * ChatUIThemeManager.addTheme(
     *     "CUSTOM_THEME",
     *     R.style.MyCustomLightTheme,
     *     R.style.MyCustomDarkTheme
     * );
     *
     * // Stack additional overrides on top of an existing theme
     * ChatUIThemeManager.addTheme(
     *     ChatUIThemeManager.LIVELY_THEME,
     *     R.style.MyOverrideLight,
     *     R.style.MyOverrideDark
     * );
     * </pre>
     *
     * @param themeType theme identifier (use UPPER_SNAKE_CASE, e.g. {@code "CUSTOM_BLUE_THEME"})
     * @param lightStyleResId style resource ID for light mode (must be non-zero)
     * @param darkStyleResId style resource ID for dark mode (must be non-zero)
     */
    public static void addTheme(String themeType, int lightStyleResId, int darkStyleResId) {
        if (themeType == null || themeType.isEmpty()) {
            RLog.e(TAG, "addTheme failed: themeType is null or empty");
            return;
        }

        if (lightStyleResId == 0 || darkStyleResId == 0) {
            RLog.e(TAG, "addTheme failed: lightStyleResId or darkStyleResId is zero");
            return;
        }

        ThemeConfig config = new ThemeConfig(lightStyleResId, darkStyleResId);

        // Get or create the config list for this theme
        List<ThemeConfig> configList = themeConfigMap.get(themeType);
        if (configList == null) {
            configList = new ArrayList<>();
            themeConfigMap.put(themeType, configList);
        }

        // Append config (stacking is intentional)
        configList.add(config);

        RLog.i(
                TAG,
                "Added theme: themeType="
                        + themeType
                        + ", lightStyleResId="
                        + lightStyleResId
                        + ", darkStyleResId="
                        + darkStyleResId
                        + ", total configs="
                        + configList.size());
    }

    /**
     * Switches to a registered built-in theme and applies it immediately.
     *
     * <p>This method:
     *
     * <ul>
     *   <li>Validates the theme is registered
     *   <li>Selects the light or dark style based on the system dark-mode state
     *   <li>Applies the theme to the Application context (globally)
     *   <li>Applies the theme to the given context (immediately)
     *   <li>Automatically applies the theme to all future Activities via lifecycle callbacks
     * </ul>
     *
     * <p>If the target theme is already active, this method is a no-op.
     *
     * <pre>
     * ChatUIThemeManager.changeInnerTheme(context, ChatUIThemeManager.LIVELY_THEME);
     * </pre>
     *
     * @param context context (Activity or Application context recommended)
     * @param themeType theme identifier ({@link #LIVELY_THEME} or any registered custom theme)
     */
    public static void changeInnerTheme(Context context, String themeType) {
        if (context == null) {
            RLog.w(TAG, "changeTheme: context is null");
            return;
        }
        if (!isInit) {
            initThemes(context);
        }

        if (themeType == null || themeType.isEmpty()) {
            RLog.w(TAG, "changeTheme: themeType is null or empty");
            return;
        }

        if (!themeConfigMap.containsKey(themeType)) {
            RLog.e(TAG, "changeTheme: theme not found: " + themeType);
            return;
        }

        if (themeType.equals(currentTheme)) {
            RLog.d(TAG, "Theme already set to: " + themeType);
            return;
        }

        RLog.i(TAG, "Changing theme from " + currentTheme + " to " + themeType);
        String oldTheme = currentTheme;
        currentTheme = themeType;
        currentBaseTheme = null; // built-in themes have no base theme

        // Apply new theme
        applyTheme(context.getApplicationContext());
        applyTheme(context);

        // Notify listeners
        notifyThemeChanged(context, oldTheme, themeType);
    }

    /**
     * Switches to a custom theme that is stacked on top of a base built-in theme.
     *
     * <p>Application order:
     *
     * <ol>
     *   <li>All style layers from {@code baseOnTheme} are applied first
     *   <li>All style layers from {@code customThemeType} are applied on top
     *   <li>Later layers override earlier ones for conflicting attributes
     * </ol>
     *
     * <pre>
     * ChatUIThemeManager.addTheme("MY_BLUE_THEME",
     *     R.style.MyBlueLightTheme, R.style.MyBlueDarkTheme);
     *
     * ChatUIThemeManager.changeCustomTheme(context, "MY_BLUE_THEME",
     *     ChatUIThemeManager.LIVELY_THEME);
     * </pre>
     *
     * @param context context (Activity or Application context recommended)
     * @param customThemeType custom theme identifier (must be registered via {@link #addTheme})
     * @param baseOnTheme base theme identifier to stack under the custom theme
     */
    public static void changeCustomTheme(
            Context context, String customThemeType, String baseOnTheme) {
        if (context == null) {
            RLog.w(TAG, "changeCustomTheme: context is null");
            return;
        }
        if (!isInit) {
            initThemes(context);
        }

        if (customThemeType == null || customThemeType.isEmpty()) {
            RLog.w(TAG, "changeCustomTheme: customThemeType is null or empty");
            return;
        }

        // Validate custom theme exists
        if (!themeConfigMap.containsKey(customThemeType)) {
            RLog.e(TAG, "changeCustomTheme: custom theme not found: " + customThemeType);
            return;
        }

        // Validate base theme exists
        if (baseOnTheme == null || !themeConfigMap.containsKey(baseOnTheme)) {
            RLog.e(TAG, "changeCustomTheme: base theme not found: " + baseOnTheme);
            return;
        }

        if (customThemeType.equals(currentTheme)) {
            RLog.d(TAG, "Theme already set to: " + customThemeType);
            return;
        }

        RLog.i(
                TAG,
                "Changing custom theme from "
                        + currentTheme
                        + " to "
                        + customThemeType
                        + " (based on "
                        + baseOnTheme
                        + ")");
        String oldTheme = currentTheme;
        currentTheme = customThemeType;
        currentBaseTheme = baseOnTheme; // saved for applying to future Activities

        // Apply new theme (applyTheme handles base + custom stacking)
        applyTheme(context.getApplicationContext());
        applyTheme(context);

        // Notify listeners
        notifyThemeChanged(context, oldTheme, customThemeType);
    }

    /**
     * Returns the currently active theme identifier.
     *
     * <pre>
     * String theme = ChatUIThemeManager.getCurrentThemeName();
     * if (ChatUIThemeManager.LIVELY_THEME.equals(theme)) {
     *     // Currently using the lively theme
     * }
     * </pre>
     *
     * @return current theme identifier (e.g. {@link #LIVELY_THEME} or a custom theme name)
     */
    public static String getCurrentThemeName() {
        return currentTheme;
    }

    /**
     * Registers a theme-change listener.
     *
     * <p>The listener is invoked on every theme switch. Duplicate registrations are silently
     * ignored.
     *
     * @param listener the listener to register (must not be {@code null})
     * @see #removeThemeListener(OnThemeListener)
     */
    public static void addThemeListener(OnThemeListener listener) {
        if (listener == null) {
            RLog.w(TAG, "addThemeChangeListener: listener is null");
            return;
        }

        if (!themeListeners.contains(listener)) {
            themeListeners.add(listener);
            RLog.d(TAG, "Theme change listener added, total listeners: " + themeListeners.size());
        } else {
            RLog.w(TAG, "Theme change listener already exists");
        }
    }

    /**
     * Unregisters a previously registered theme-change listener.
     *
     * @param listener the listener to remove (must not be {@code null})
     * @see #addThemeListener(OnThemeListener)
     */
    public static void removeThemeListener(OnThemeListener listener) {
        if (listener == null) {
            RLog.w(TAG, "removeThemeChangeListener: listener is null");
            return;
        }

        boolean removed = themeListeners.remove(listener);
        if (removed) {
            RLog.d(
                    TAG,
                    "Theme change listener removed, remaining listeners: " + themeListeners.size());
        } else {
            RLog.w(TAG, "Theme change listener not found");
        }
    }

    // ========== Resource resolution ==========

    /**
     * Resolves a theme attribute to its actual resource ID.
     *
     * @param context context (must not be {@code null})
     * @param attrId theme attribute ID (e.g. {@code R.attr.nc_xxx})
     * @return the resolved resource ID, or {@code 0} if resolution fails
     */
    public static int getAttrResId(Context context, int attrId) {
        if (context == null || attrId == 0) {
            return 0;
        }
        if (!isInit) {
            initThemes(context.getApplicationContext());
        }
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attrId, typedValue, true)) {
            return typedValue.resourceId;
        }
        Context applicationContext = context.getApplicationContext();
        if (applicationContext != null) {
            if (applicationContext.getTheme().resolveAttribute(attrId, typedValue, true)) {
                return typedValue.resourceId;
            }
        }
        return typedValue.resourceId;
    }

    /**
     * Resolves a theme color attribute to an ARGB color integer.
     *
     * @param context context (must not be {@code null})
     * @param attrId color attribute ID (e.g. {@code R.attr.nc_text_primary})
     * @return resolved ARGB color, or {@code 0} if resolution fails
     */
    public static int getColorFromAttrId(Context context, int attrId) {
        if (context == null || attrId == 0) {
            return 0;
        }
        int colorResId = getAttrResId(context, attrId);
        if (colorResId == 0) {
            return 0;
        }
        try {
            return context.getResources().getColor(colorResId);
        } catch (Resources.NotFoundException e) {
            RLog.e(TAG, "getColorFromAttrId failed, attrId=" + attrId, e);
            return 0;
        }
    }

    // ========== Private helpers ==========

    /** Notifies all registered listeners that the theme has changed. */
    private static void notifyThemeChanged(Context context, String oldTheme, String newTheme) {
        if (themeListeners.isEmpty()) {
            return;
        }

        RLog.d(TAG, "Notifying " + themeListeners.size() + " theme change listeners");

        for (OnThemeListener listener : themeListeners) {
            if (listener != null) {
                try {
                    listener.onThemeChanged(context, oldTheme, newTheme);
                } catch (Exception e) {
                    RLog.e(TAG, "Error notifying theme change listener", e);
                }
            }
        }
    }

    /** Initializes the theme manager and registers the Activity lifecycle callback. */
    private static void initializeThemeManager(Context context) {
        if (context == null) {
            RLog.w(TAG, "initializeThemeManager: context is null");
            return;
        }

        Context appContext = context.getApplicationContext();

        RLog.i(TAG, "ThemeManager initialized");

        // Register lifecycle callback to auto-apply theme to all Activities
        if (appContext instanceof Application) {
            ((Application) appContext).registerActivityLifecycleCallbacks(new ThemeCallback());
        }

        // Apply theme to Application context
        applyTheme(appContext);
    }

    /** Returns whether the system is currently in dark mode. */
    public static boolean isSystemInDarkMode(Context context) {
        if (context == null) {
            return false;
        }
        int nightMode =
                context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Applies the current theme (and its base theme if stacked) to the given context. */
    private static void applyTheme(Context context) {
        if (context == null) {
            RLog.w(TAG, "applyTheme: context is null");
            return;
        }

        Resources.Theme theme = context.getTheme();
        if (theme == null) {
            context.setTheme(android.R.style.Theme_Light);
            theme = context.getTheme();
        }

        // Select style variant based on current dark-mode state
        boolean isDarkMode = isSystemInDarkMode(context);

        // Apply the base theme layers first (if this is a stacked custom theme)
        int totalAppliedCount = 0;
        if (currentBaseTheme != null) {
            List<ThemeConfig> baseConfigList = themeConfigMap.get(currentBaseTheme);
            if (baseConfigList != null && !baseConfigList.isEmpty()) {
                for (ThemeConfig config : baseConfigList) {
                    int styleResId = isDarkMode ? config.darkStyleResId : config.lightStyleResId;
                    try {
                        theme.applyStyle(styleResId, true);
                        totalAppliedCount++;
                        RLog.v(
                                TAG,
                                "Applied base style: "
                                        + styleResId
                                        + " for base theme: "
                                        + currentBaseTheme);
                    } catch (Exception e) {
                        RLog.e(TAG, "Failed to apply base theme style: " + styleResId, e);
                    }
                }
            }
        }

        // Apply the current theme layers on top
        List<ThemeConfig> configList = themeConfigMap.get(currentTheme);
        if (configList == null || configList.isEmpty()) {
            RLog.w(TAG, "No theme config found for: " + currentTheme);
            return;
        }

        int currentAppliedCount = 0;
        for (ThemeConfig config : configList) {
            int styleResId = isDarkMode ? config.darkStyleResId : config.lightStyleResId;
            try {
                theme.applyStyle(styleResId, true);
                currentAppliedCount++;
                RLog.v(TAG, "Applied style: " + styleResId + " for theme: " + currentTheme);
            } catch (Exception e) {
                RLog.e(TAG, "Failed to apply theme style: " + styleResId, e);
            }
        }

        totalAppliedCount += currentAppliedCount;

        if (currentBaseTheme != null) {
            RLog.d(
                    TAG,
                    "Applied custom theme: "
                            + currentTheme
                            + " (based on "
                            + currentBaseTheme
                            + "), isDarkMode="
                            + isDarkMode
                            + ", applied "
                            + totalAppliedCount
                            + " styles");
        } else {
            RLog.d(
                    TAG,
                    "Applied theme: "
                            + currentTheme
                            + ", isDarkMode="
                            + isDarkMode
                            + ", applied "
                            + totalAppliedCount
                            + " of "
                            + configList.size()
                            + " styles");
        }
    }

    // ========== Inner classes ==========

    /**
     * ActivityLifecycleCallbacks that auto-applies the current theme when any Activity is created.
     */
    private static class ThemeCallback implements Application.ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(
                @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            RLog.v(TAG, "onActivityCreated: " + activity.getClass().getSimpleName());
            ChatUIThemeManager.applyTheme(activity);
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            // no-op
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            // no-op
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            // no-op
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            // no-op
        }

        @Override
        public void onActivitySaveInstanceState(
                @NonNull Activity activity, @NonNull Bundle outState) {
            // no-op
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            // no-op
        }
    }
}
