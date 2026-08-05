package ai.nexconn.chatui.emoji;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji2.bundled.BundledEmojiCompatConfig;
import androidx.emoji2.text.EmojiCompat;

/**
 * @author gusd @Date 2022/04/19
 */
public class EmojiInitProvider extends ContentProvider {
    private static final String TAG = "EmojiInitProvider";

    @Override
    public boolean onCreate() {
        initEmoji();
        return false;
    }

    private void initEmoji() {
        BundledEmojiCompatConfig config = new BundledEmojiCompatConfig(getContext());
        config.setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL).setReplaceAll(true);
        EmojiCompat.init(config);
    }

    @Nullable
    @Override
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(
            @NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(
            @NonNull Uri uri,
            @Nullable ContentValues values,
            @Nullable String selection,
            @Nullable String[] selectionArgs) {
        return 0;
    }
}
