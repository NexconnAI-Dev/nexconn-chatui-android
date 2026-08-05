package ai.nexconn.chatui.channel.readreceipt;

import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.ReadReceiptInfo;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseActivity;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Group message read-status detail page.
 *
 * @since 5.30.0
 */
public class MessageReadDetailActivity extends BaseActivity {

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull Message message,
            @Nullable ReadReceiptInfo receiptInfo) {
        Intent intent = new Intent(context, MessageReadDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatUIConstants.KEY_RECEIPT_MESSAGE_ID, message.getMessageId());
        bundle.putString(
                ChatUIConstants.KEY_RECEIPT_CHANNEL_ID,
                message.getChannelIdentifier().getChannelId());
        bundle.putInt(
                ChatUIConstants.KEY_RECEIPT_CHANNEL_TYPE,
                message.getChannelIdentifier().getChannelType().getValue());
        bundle.putString(ChatUIConstants.KEY_RECEIPT_SENDER_USER_ID, message.getSenderUserId());
        bundle.putLong(ChatUIConstants.KEY_RECEIPT_SENT_TIME, message.getSentTime());
        if (receiptInfo != null) {
            bundle.putInt(ChatUIConstants.KEY_RECEIPT_READ_COUNT, receiptInfo.getReadCount());
            bundle.putInt(ChatUIConstants.KEY_RECEIPT_UNREAD_COUNT, receiptInfo.getUnreadCount());
            bundle.putInt(ChatUIConstants.KEY_RECEIPT_TOTAL_COUNT, receiptInfo.getTotalCount());
        }
        intent.putExtras(bundle);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nc_activity);

        Fragment fragment = createFragment();
        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStack();
        manager.beginTransaction().replace(R.id.fl_fragment_container, fragment).commit();
    }

    @NonNull
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle();
        return NCChatUI.getFragmentFactory().newMessageReadDetailFragment(bundle);
    }
}
