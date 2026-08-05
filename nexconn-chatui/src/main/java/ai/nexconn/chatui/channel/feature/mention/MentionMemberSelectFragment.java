package ai.nexconn.chatui.channel.feature.mention;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.utils.text.CharacterParser;
import ai.nexconn.chatui.widget.SideBar;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @ mention member selection Fragment, logic migrated from MentionMemberSelectActivity.
 */
public class MentionMemberSelectFragment extends Fragment {

    private ListView mListView;
    private List<MemberInfo> mAllMemberList;
    private MembersAdapter mAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.nc_mention_select_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText searchBar = view.findViewById(R.id.nc_edit_text);
        mListView = view.findViewById(R.id.nc_list);
        SideBar mSideBar = view.findViewById(R.id.nc_sidebar);
        TextView letterPopup = view.findViewById(R.id.nc_popup_bg);
        mSideBar.setTextView(letterPopup);
        mAdapter = new MembersAdapter();
        mListView.setAdapter(mAdapter);
        mAllMemberList = new ArrayList<>();
        Bundle args = getArguments();
        String targetId = args != null ? args.getString(RouteUtils.TARGET_ID) : null;
        ChannelType conversationType = ChannelType.GROUP;
        if (args != null && args.containsKey(RouteUtils.CHANNEL_TYPE)) {
            conversationType =
                    ChannelType.Companion.fromValue(
                            args.getInt(RouteUtils.CHANNEL_TYPE, ChannelType.GROUP.getValue()));
        }
        final String finalTargetId = targetId;
        NCMentionManager.IGroupMembersProvider provider =
                NCMentionManager.getInstance().getGroupMembersProvider();
        if (conversationType.equals(ChannelType.GROUP)
                && provider != null
                && finalTargetId != null) {
            provider.getGroupMembers(
                    finalTargetId,
                    new NCMentionManager.IGroupMemberCallback() {
                        @Override
                        public void onGetGroupMembersResult(final List<UserInfo> members) {
                            if (members == null || members.isEmpty()) return;
                            handler.post(
                                    () -> {
                                        for (UserInfo userInfo : members) {
                                            if (userInfo == null
                                                    || userInfo.getUserId()
                                                            .equals(NCEngine.getCurrentUserId()))
                                                continue;
                                            MemberInfo mi = new MemberInfo(userInfo);
                                            String pinyin =
                                                    CharacterParser.getInstance()
                                                            .getSelling(userInfo.getName());
                                            String sort =
                                                    (pinyin != null && pinyin.length() > 0)
                                                            ? pinyin.substring(0, 1).toUpperCase()
                                                            : "#";
                                            mi.setLetter(
                                                    sort.matches("[A-Z]")
                                                            ? sort.toUpperCase()
                                                            : "#");
                                            mAllMemberList.add(mi);
                                        }
                                        Collections.sort(
                                                mAllMemberList, PinyinComparator.getInstance());
                                        mAdapter.setData(mAllMemberList);
                                        mAdapter.notifyDataSetChanged();
                                    });
                        }
                    });
        }
        mListView.setOnItemClickListener(
                (parent, v, position, id) -> {
                    if (getActivity() != null) getActivity().finish();
                    MemberInfo item = mAdapter.getItem(position);
                    if (item != null && item.userInfo != null)
                        NCMentionManager.getInstance().mentionMember(item.userInfo);
                });
        mSideBar.setOnTouchingLetterChangedListener(
                s -> {
                    int pos = mAdapter.getPositionForSection(s.charAt(0));
                    if (pos != -1) mListView.setSelection(pos);
                });
        searchBar.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        List<MemberInfo> filter = new ArrayList<>();
                        if (TextUtils.isEmpty(s.toString())) filter = mAllMemberList;
                        else {
                            for (MemberInfo m : mAllMemberList) {
                                String name = m.userInfo.getName();
                                if (name != null
                                        && (name.contains(s)
                                                || CharacterParser.getInstance()
                                                        .getSelling(name)
                                                        .startsWith(s.toString()))) filter.add(m);
                            }
                        }
                        Collections.sort(filter, PinyinComparator.getInstance());
                        mAdapter.setData(filter);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
        view.findViewById(R.id.nc_btn_cancel)
                .setOnClickListener(
                        v -> {
                            if (getActivity() != null) getActivity().finish();
                        });
    }

    public static class PinyinComparator implements Comparator<MemberInfo> {
        private static PinyinComparator instance;

        public static PinyinComparator getInstance() {
            if (instance == null) instance = new PinyinComparator();
            return instance;
        }

        @Override
        public int compare(MemberInfo o1, MemberInfo o2) {
            if ("@".equals(o1.getLetter()) || "#".equals(o2.getLetter())) return -1;
            if ("#".equals(o1.getLetter()) || "@".equals(o2.getLetter())) return 1;
            return o1.getLetter().compareTo(o2.getLetter());
        }
    }

    private class MembersAdapter extends BaseAdapter implements SectionIndexer {
        private List<MemberInfo> mList = new ArrayList<>();

        void setData(List<MemberInfo> list) {
            mList = list;
        }

        @Override
        public Object[] getSections() {
            return new Object[0];
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            for (int i = 0; i < getCount(); i++) {
                if (mList.get(i).getLetter().toUpperCase().charAt(0) == sectionIndex) return i;
            }
            return -1;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mList.get(position).getLetter().charAt(0);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public MemberInfo getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh;
            if (convertView == null) {
                vh = new ViewHolder();
                convertView =
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.nc_mention_list_item, null);
                vh.name = convertView.findViewById(R.id.nc_user_name);
                vh.portrait = convertView.findViewById(R.id.nc_user_portrait);
                vh.letter = convertView.findViewById(R.id.letter);
                convertView.setTag(vh);
            } else vh = (ViewHolder) convertView.getTag();
            UserInfo ui = mList.get(position).userInfo;
            if (ui != null) {
                vh.name.setText(ui.getName());
                Glide.with(convertView).load(ui.getPortraitUri()).into(vh.portrait);
            }
            int section = getSectionForPosition(position);
            if (position == getPositionForSection(section)) {
                vh.letter.setVisibility(View.VISIBLE);
                vh.letter.setText(mList.get(position).getLetter());
            } else vh.letter.setVisibility(View.GONE);
            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView portrait;
        TextView name;
        TextView letter;
    }

    private static class MemberInfo {
        final UserInfo userInfo;
        String letter;

        MemberInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
        }

        String getLetter() {
            return letter;
        }

        void setLetter(String letter) {
            this.letter = letter;
        }
    }
}
