package ai.nexconn.chatui.channel.feature.combineforward;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.CombineMessage;
import ai.nexconn.chatui.R;
import android.content.Context;
import java.util.List;

/** Utility for building combine-forward titles consistently across list/detail pages. */
public final class CombineForwardTitleHelper {

    private CombineForwardTitleHelper() {
        // no instances
    }

    public static String buildTitle(Context context, CombineMessage combineMessage) {
        if (context == null) {
            return "Chat history";
        }
        String defaultTitle = context.getString(R.string.nc_combine_chat_record);
        if (combineMessage == null) {
            return defaultTitle;
        }
        ChannelType sourceType = combineMessage.getChannelType();
        if (sourceType == ChannelType.GROUP || sourceType == ChannelType.COMMUNITY) {
            return context.getString(R.string.nc_combine_group_chat);
        }
        List<String> nameList = combineMessage.getNameList();
        if (nameList == null || nameList.isEmpty()) {
            return defaultTitle;
        }
        if (nameList.size() == 1) {
            return context.getString(R.string.nc_combine_the_group_chat_of, nameList.get(0));
        }
        if (nameList.size() == 2) {
            String joinedName =
                    nameList.get(0) + context.getString(R.string.nc_combine_and) + nameList.get(1);
            return context.getString(R.string.nc_combine_the_group_chat_of, joinedName);
        }
        return defaultTitle;
    }
}
