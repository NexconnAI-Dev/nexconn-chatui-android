package ai.nexconn.chatui.utils.route;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.message.FileMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.activity.CombinePicturePagerActivity;
import ai.nexconn.chatui.activity.FilePreviewActivity;
import ai.nexconn.chatui.activity.NCWebviewActivity;
import ai.nexconn.chatui.activity.WebFilePreviewActivity;
import ai.nexconn.chatui.channel.ChannelActivity;
import ai.nexconn.chatui.channel.ChannelFragment;
import ai.nexconn.chatui.channel.feature.combineforward.CombineMessagePreviewActivity;
import ai.nexconn.chatui.channel.feature.forward.ForwardClickActions;
import ai.nexconn.chatui.channel.feature.mention.MentionMemberSelectActivity;
import ai.nexconn.chatui.channel.subchannel.SubChannelListActivity;
import ai.nexconn.chatui.channellist.ChannelListActivity;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.message.MessageHolder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.HashMap;

public class RouteUtils {

    private static final String TAG = "RouteUtils";

    public static final String CHANNEL_TYPE = "ChannelType";
    public static final String TARGET_ID = "targetId";
    public static final String CHANNEL_ID = "channelId";
    public static final String CHANNEL_IDENTIFIER = "ChannelIdentifier";
    public static final String CREATE_CHATROOM = "createIfNotExist";
    public static final String TITLE = "title";
    public static final String INDEX_MESSAGE_TIME = "indexTime";
    public static final String CUSTOM_SERVICE_INFO = "customServiceInfo";
    public static final String FORWARD_TYPE = "forwardType";
    public static final String MESSAGE_IDS = "messageIds";
    public static final String MESSAGE_ID = "messageId";
    public static final String MESSAGE = "message";
    public static final String DISABLE_SYSTEM_EMOJI = "disableSystemEmoji";
    private static HashMap<ChatUIActivityType, Class<? extends Activity>> sActivityMap =
            new HashMap<>();

    public static void routeToChannelListActivity(Context context, String title) {
        if (context == null) {
            RLog.e(TAG, "routeToChannelListActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = ChannelListActivity.class;
        if (sActivityMap.get(ChatUIActivityType.ChannelListActivity) != null) {
            activity = sActivityMap.get(ChatUIActivityType.ChannelListActivity);
        }
        Intent intent = new Intent(context, activity);
        if (!TextUtils.isEmpty(title)) {
            intent.putExtra(TITLE, title);
        }
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static void routeToChannelActivity(Context context, ChannelType type, String targetId) {
        ChannelIdentifier identifier = new ChannelIdentifier(type, targetId);
        routeToChannelActivity(context, identifier, false, null);
    }

    public static void routeToChannelActivity(
            Context context, ChannelType type, String targetId, boolean disableSystemEmoji) {
        ChannelIdentifier identifier = new ChannelIdentifier(type, targetId);
        routeToChannelActivity(context, identifier, disableSystemEmoji, null);
    }

    /**
     * Launches the channel (conversation) activity.
     *
     * @param context context
     * @param type channel type
     * @param targetId target ID
     * @param bundle extra bundle data to carry in the intent
     */
    public static void routeToChannelActivity(
            Context context, ChannelType type, String targetId, Bundle bundle) {
        ChannelIdentifier identifier = new ChannelIdentifier(type, targetId);
        routeToChannelActivity(context, identifier, false, bundle);
    }

    /**
     * Launches the channel (conversation) activity.
     *
     * @param context context
     * @param type channel type
     * @param targetId target ID
     * @param disableSystemEmoji whether to hide built-in emoji
     * @param bundle extra bundle data to carry in the intent
     */
    public static void routeToChannelActivity(
            Context context,
            ChannelType type,
            String targetId,
            boolean disableSystemEmoji,
            Bundle bundle) {
        ChannelIdentifier identifier = new ChannelIdentifier(type, targetId);
        routeToChannelActivity(context, identifier, disableSystemEmoji, bundle);
    }

    /**
     * Launches the channel (conversation) activity.
     *
     * @param context context
     * @param channelIdentifier channel identifier
     */
    public static void routeToChannelActivity(
            Context context, ChannelIdentifier channelIdentifier) {
        routeToChannelActivity(context, channelIdentifier, false, null);
    }

    /**
     * Launches the channel (conversation) activity.
     *
     * @param context context
     * @param channelIdentifier channel identifier
     * @param bundle extra bundle data to carry in the intent
     */
    public static void routeToChannelActivity(
            Context context, ChannelIdentifier channelIdentifier, Bundle bundle) {
        routeToChannelActivity(context, channelIdentifier, false, bundle);
    }

    /**
     * Launches the channel (conversation) activity.
     *
     * @param context context
     * @param channelIdentifier channel identifier
     * @param disableSystemEmoji whether to hide built-in emoji
     * @param bundle extra bundle data to carry in the intent
     */
    public static void routeToChannelActivity(
            Context context,
            ChannelIdentifier channelIdentifier,
            boolean disableSystemEmoji,
            Bundle bundle) {
        if (context == null) {
            RLog.e(TAG, "routeToChannelActivity: context is null");
            return;
        }
        if (channelIdentifier == null) {
            RLog.e(TAG, "routeToChannelActivity: channelIdentifier is empty");
            return;
        }
        if (TextUtils.isEmpty(channelIdentifier.getChannelId())) {
            RLog.e(TAG, "routeToChannelActivity: channelId is empty");
            return;
        }
        if (channelIdentifier.getChannelType() == null) {
            RLog.e(TAG, "routeToChannelActivity: type is empty");
            return;
        }
        Class<? extends Activity> activity = ChannelActivity.class;
        if (sActivityMap.get(ChatUIActivityType.ChannelActivity) != null) {
            activity = sActivityMap.get(ChatUIActivityType.ChannelActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.putExtra(TARGET_ID, channelIdentifier.getChannelId());
        intent.putExtra(CHANNEL_TYPE, channelIdentifier.getChannelType().name().toLowerCase());
        intent.putExtra(DISABLE_SYSTEM_EMOJI, disableSystemEmoji);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * Launches the sub-channel list (grouped conversations) activity.
     *
     * @param context context
     * @param type grouped channel type
     * @param title title
     */
    public static void routeToSubChannelListActivity(
            Context context, ChannelType type, String title) {
        if (context == null) {
            RLog.e(TAG, "routeToSubChannelListActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = SubChannelListActivity.class;
        if (sActivityMap.get(ChatUIActivityType.SubChannelListActivity) != null) {
            activity = sActivityMap.get(ChatUIActivityType.SubChannelListActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.putExtra(CHANNEL_TYPE, type);
        intent.putExtra(TITLE, title);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * Launches the @ mention member selection activity.
     *
     * @param context context
     * @param targetId target ID
     * @param type channel type
     */
    public static void routeToMentionMemberSelectActivity(
            Context context, String targetId, ChannelType type) {
        if (context == null) {
            RLog.e(TAG, "routeToMentionMemberSelectActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = MentionMemberSelectActivity.class;
        if (sActivityMap.get(ChatUIActivityType.MentionMemberSelectActivity) != null) {
            activity = sActivityMap.get(ChatUIActivityType.MentionMemberSelectActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.putExtra(CHANNEL_TYPE, type.getValue());
        intent.putExtra(TARGET_ID, targetId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void routeToWebActivity(Context context, String url) {
        routeToWebActivity(context, url, null);
    }

    /**
     * Launches the web view activity.
     *
     * @param context context
     * @param url remote URL
     * @param title title
     */
    public static void routeToWebActivity(Context context, String url, String title) {
        if (context == null) {
            RLog.e(TAG, "routeToWebActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = NCWebviewActivity.class;
        if (sActivityMap.get(ChatUIActivityType.ChatUIWebViewActivity) != null) {
            activity = sActivityMap.get(ChatUIActivityType.ChatUIWebViewActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static void routeToFilePreviewActivity(
            Context context, Message message, FileMessage content, int progress) {
        if (context == null) {
            RLog.e(TAG, "routeToFilePreviewActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = FilePreviewActivity.class;
        if (sActivityMap.get(ChatUIActivityType.FilePreviewActivity) != null) {
            activity = sActivityMap.get(ChatUIActivityType.FilePreviewActivity);
        }
        Intent intent = new Intent(context, activity);
        MessageHolder.holdContent(content);
        MessageHolder.holdMessage(message);
        intent.putExtra("Progress", progress);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * Launches the channel selection activity for message forwarding.
     *
     * @param fragment current fragment
     * @param type forward type. {@link ForwardClickActions.ForwardType}
     * @param messageIds list of message IDs to forward
     */
    public static void routeToForwardSelectChannelActivity(
            Fragment fragment,
            ForwardClickActions.ForwardType type,
            ArrayList<Integer> messageIds) {
        if (fragment == null || fragment.getContext() == null) {
            RLog.e(TAG, "routeToForwardSelectChannelActivity: fragment or context is null");
            return;
        }
        Class<? extends Activity> activity =
                sActivityMap.get(ChatUIActivityType.ForwardSelectChannelActivity);
        if (activity == null) {
            RLog.e(
                    TAG,
                    "routeToForwardSelectChannelActivity: no activity registered for forwarding.");
            return;
        }
        Intent intent = new Intent(fragment.getContext(), activity);
        intent.putExtra(FORWARD_TYPE, type.getValue());
        intent.putIntegerArrayListExtra(MESSAGE_IDS, messageIds);
        if (!(fragment.getContext() instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        fragment.startActivityForResult(intent, ChannelFragment.REQUEST_CODE_FORWARD);
    }

    /**
     * Launches the combined forward message detail preview activity.
     *
     * @param context context (uses NCChatUI context if null)
     * @param message combined forward message
     */
    public static void routeToCombineMessageDetailActivity(Context context, Message message) {
        Context ctx = context;
        if (ctx == null) {
            ctx = ai.nexconn.chatui.NCChatUI.getContext();
        }
        if (ctx == null) {
            RLog.e(TAG, "routeToCombineMessageDetailActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = CombineMessagePreviewActivity.class;
        if (sActivityMap.get(ChatUIActivityType.CombineMessageDetailActivity) != null) {
            activity = sActivityMap.get(ChatUIActivityType.CombineMessageDetailActivity);
        }
        Intent intent = new Intent(ctx, activity);
        intent.setPackage(ctx.getApplicationContext().getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MessageHolder.holdMessage(message);
        ctx.startActivity(intent);
    }

    /**
     * Launches the image viewer page for combined forward messages.
     *
     * @param context context
     * @param message the original message carried during combined forwarding
     */
    public static void routeToCombinePicturePagerActivity(Context context, Message message) {
        if (context == null) {
            RLog.e(TAG, "routeToCombinePicturePagerActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = CombinePicturePagerActivity.class;
        if (sActivityMap.get(ChatUIActivityType.CombinePicturePagerActivity) != null) {
            activity = sActivityMap.get(ChatUIActivityType.CombinePicturePagerActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.setPackage(context.getApplicationContext().getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MessageHolder.holdMessage(message);
        context.startActivity(intent);
    }

    /**
     * Launches the online file preview activity.
     *
     * @param context context
     * @param fileUrl remote file URL
     * @param fileName file name
     * @param fileSize file size
     */
    public static void routeToWebFilePreviewActivity(
            Context context, String fileUrl, String fileName, String fileSize) {
        if (context == null) {
            RLog.e(TAG, "routeToWebFilePreviewActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = WebFilePreviewActivity.class;
        if (sActivityMap.get(ChatUIActivityType.WebFilePreviewActivity) != null) {
            activity = sActivityMap.get(ChatUIActivityType.WebFilePreviewActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.setPackage(context.getPackageName());
        intent.putExtra("fileUrl", fileUrl);
        intent.putExtra("fileName", fileName);
        intent.putExtra("fileSize", fileSize);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static void registerActivity(
            ChatUIActivityType activityType, Class<? extends Activity> activity) {
        sActivityMap.put(activityType, activity);
    }

    public static Class<? extends Activity> getActivity(ChatUIActivityType type) {
        return sActivityMap.get(type);
    }

    public enum ChatUIActivityType {
        ChannelListActivity,
        SubChannelListActivity,
        ChannelActivity,
        MentionMemberSelectActivity,
        ChatUIWebViewActivity,
        FilePreviewActivity,
        CombinePicturePagerActivity,
        CombineMessageDetailActivity,
        ForwardSelectChannelActivity,
        WebFilePreviewActivity,
    }
}
