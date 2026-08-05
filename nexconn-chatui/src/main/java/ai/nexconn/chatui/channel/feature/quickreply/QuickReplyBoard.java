package ai.nexconn.chatui.channel.feature.quickreply;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chat.params.SendMessageParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.extension.NCExtension;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.List;

public class QuickReplyBoard {
    private final AdapterView.OnItemClickListener mListener;
    private ListView mListView;
    private List<String> mPhraseList;
    private View mRootView;
    private ChannelIdentifier mChannelIdentifier;

    public QuickReplyBoard(
            @NonNull Context context,
            ViewGroup parent,
            List<String> phraseList,
            AdapterView.OnItemClickListener listener) {
        mPhraseList = phraseList;
        mListener = listener;
        initView(context, parent);
    }

    private void initView(Context context, ViewGroup parent) {
        mRootView =
                LayoutInflater.from(context)
                        .inflate(R.layout.nc_ext_quick_reply_list_v2, parent, false);
        mListView = mRootView.findViewById(R.id.nc_list);
        PhrasesAdapter adapter = new PhrasesAdapter();
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        sendMessage(mPhraseList.get(position));
                        if (mListener != null) {
                            mListener.onItemClick(parent, view, position, id);
                        }
                    }
                });
    }

    private void sendMessage(String text) {
        TextMessage textMessage = new TextMessage(text);
        NCChatUI.sendMessage(mChannelIdentifier, new SendMessageParams(textMessage), null);
    }

    public void setAttachedConversation(NCExtension extension) {
        if (extension != null) {
            mChannelIdentifier = extension.getChannelIdentifier();
        }
    }

    public View getRootView() {
        return mRootView;
    }

    private class PhrasesAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mPhraseList.size();
        }

        @Override
        public Object getItem(int position) {
            return mPhraseList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                Context context = parent.getContext();
                // Choose list item layout based on theme
                convertView =
                        LayoutInflater.from(context)
                                .inflate(R.layout.nc_ext_quick_reply_list_item_v2, parent, false);
            }
            TextView tvPhrases = convertView.findViewById(R.id.nc_phrases_tv);
            tvPhrases.setText(mPhraseList.get(position));
            return convertView;
        }
    }
}
