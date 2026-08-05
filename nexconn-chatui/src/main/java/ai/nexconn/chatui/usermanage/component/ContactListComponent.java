package ai.nexconn.chatui.usermanage.component;

import ai.nexconn.chat.user.model.UserOnlineStatus;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseComponent;
import ai.nexconn.chatui.base.adapter.HeaderAndFooterWrapper;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.model.OnlineStatusFriendInfo;
import ai.nexconn.chatui.usermanage.adapter.ContactListAdapter;
import ai.nexconn.chatui.widget.SideBar;
import ai.nexconn.chatui.widget.pullrefresh.SmartRefreshLayout;
import ai.nexconn.chatui.widget.pullrefresh.wrapper.NCRefreshHeader;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContactListComponent extends BaseComponent {

    private RecyclerView rvContactList;
    private SideBar sideBarContact;
    private boolean showSideBar;
    private boolean showItemSelectButton;
    private boolean showItemRightArrow;
    private boolean showItemRightText;
    private boolean showDivider;
    private boolean showItemSelectAutoUpdate;
    private boolean showItemRemoveButton;
    private SmartRefreshLayout refreshLayout;
    private ContactListAdapter contactListAdapter;
    private HeaderAndFooterWrapper headerAndFooterWrapper;
    private OnActionClickListener<ContactModel> onItemClickListener;
    private OnPagedDataLoader onPagedDataLoader;
    private OnActionClickListener<ContactModel> onItemRemoveClickListener;

    public ContactListComponent(@NonNull Context context) {
        super(context);
    }

    public ContactListComponent(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactListComponent(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateView(
            Context context,
            LayoutInflater inflater,
            @NonNull ViewGroup parent,
            AttributeSet attrs) {
        // Inflate the layout and attach it to the parent ViewGroup
        View view = inflater.inflate(R.layout.nc_contact_list_component, parent, false);

        // Process custom attributes
        if (attrs != null) {
            TypedArray a =
                    context.getTheme()
                            .obtainStyledAttributes(attrs, R.styleable.ContactListComponent, 0, 0);
            try {
                showSideBar = a.getBoolean(R.styleable.ContactListComponent_show_side_bar, false);
                showItemSelectButton =
                        a.getBoolean(R.styleable.ContactListComponent_show_item_select_icon, false);
                showItemRightArrow =
                        a.getBoolean(R.styleable.ContactListComponent_show_item_right_arrow, false);
                showItemRightText =
                        a.getBoolean(R.styleable.ContactListComponent_show_item_right_text, false);
                showDivider = a.getBoolean(R.styleable.ContactListComponent_show_divider, false);
                showItemSelectAutoUpdate =
                        a.getBoolean(
                                R.styleable.ContactListComponent_show_item_select_auto_update,
                                false);
                showItemRemoveButton =
                        a.getBoolean(
                                R.styleable.ContactListComponent_show_item_remove_button, false);
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        }

        refreshLayout = view.findViewById(R.id.nc_refresh);
        refreshLayout.setNestedScrollingEnabled(false);
        refreshLayout.setRefreshHeader(new NCRefreshHeader(context));
        refreshLayout.setRefreshFooter(new NCRefreshHeader(context));
        refreshLayout.setEnableRefresh(false);
        refreshLayout.setEnableLoadMore(false);
        refreshLayout.setOnLoadMoreListener(
                refreshLayout -> {
                    if (onPagedDataLoader != null) {
                        onPagedDataLoader.loadNext(
                                aBoolean -> {
                                    refreshLayout.finishLoadMore();
                                    if (!onPagedDataLoader.hasNext()) {
                                        refreshLayout.setEnableLoadMore(false);
                                    }
                                });
                    }
                });

        // Initialize SideBar and its related views
        sideBarContact = view.findViewById(R.id.side_bar_contact);
        sideBarContact.setVisibility(showSideBar ? VISIBLE : GONE);
        TextView groupDialogTextView = view.findViewById(R.id.tv_group_overlay);
        groupDialogTextView.setVisibility(GONE);
        sideBarContact.setTextView(groupDialogTextView);
        sideBarContact.setOnTouchingLetterChangedListener(
                s -> {
                    if (contactListAdapter != null && rvContactList != null && s != null) {
                        int position = contactListAdapter.getPositionForSection(s.charAt(0));
                        if (position != -1) {
                            LinearLayoutManager layoutManager =
                                    (LinearLayoutManager) rvContactList.getLayoutManager();
                            if (layoutManager != null) {
                                // Scroll the specified position to the top of the RecyclerView
                                layoutManager.scrollToPositionWithOffset(position, 0);
                            }
                        }
                    }
                });

        // Initialize RecyclerView
        rvContactList = view.findViewById(R.id.rv_contact_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        rvContactList.setLayoutManager(layoutManager);

        // Disable change animation to prevent portrait flicker
        if (rvContactList.getItemAnimator() != null) {
            rvContactList.getItemAnimator().setChangeDuration(0);
        }

        // Set item divider if enabled
        if (showDivider) {
            DividerItemDecoration itemDecoration =
                    new DividerItemDecoration(context, layoutManager.getOrientation());
            rvContactList.addItemDecoration(itemDecoration);
        }

        // Initialize and set the adapter
        contactListAdapter =
                new ContactListAdapter(
                        showItemSelectButton,
                        showItemRightArrow,
                        showItemRightText,
                        showItemSelectAutoUpdate,
                        showItemRemoveButton);
        contactListAdapter.setOnItemClickListener(
                new OnActionClickListener<ContactModel>() {
                    @Override
                    public void onActionClick(ContactModel contactModel) {}

                    @Override
                    public <E> void onActionClickWithConfirm(
                            ContactModel contactModel, OnConfirmClickListener<E> listener) {
                        OnActionClickListener.super.onActionClickWithConfirm(
                                contactModel, listener);
                        if (onItemClickListener != null) {
                            onItemClickListener.onActionClickWithConfirm(contactModel, listener);
                        }
                    }
                });
        contactListAdapter.setOnItemRemoveClickListener(
                contactModel -> {
                    if (onItemRemoveClickListener != null) {
                        onItemRemoveClickListener.onActionClick(contactModel);
                    }
                });
        headerAndFooterWrapper = new HeaderAndFooterWrapper(contactListAdapter);
        rvContactList.setAdapter(headerAndFooterWrapper);
        return view;
    }

    /**
     * Sets the contact list data.
     *
     * @param data the contact list data
     */
    public void setContactList(List<ContactModel> data) {
        if (contactListAdapter != null) {
            contactListAdapter.setData(data);
        }
        if (headerAndFooterWrapper != null) {
            headerAndFooterWrapper.notifyDataSetChanged();
        }

        if (onPagedDataLoader != null) {
            refreshLayout.setEnableLoadMore(onPagedDataLoader.hasNext());
        }

        if (data != null && !data.isEmpty()) {
            List<String> lettersList = new ArrayList<>();
            for (ContactModel contactModel : data) {
                if (contactModel.getContactType() == ContactModel.ItemType.TITLE
                        && contactModel.getBean() instanceof String) {
                    lettersList.add((String) contactModel.getBean());
                }
            }
            setSideBarContactLetters(lettersList.toArray(new String[0]));
        }
    }

    /** Sets the online status for the contact list. */
    public void setContactOnlineStatusList(Map<String, UserOnlineStatus> statusMap) {
        if (contactListAdapter == null || headerAndFooterWrapper == null) {
            return;
        }
        List<ContactModel> data = contactListAdapter.getData();
        if (data == null || data.isEmpty()) {
            return;
        }
        int headersCount = headerAndFooterWrapper.getHeadersCount();
        for (int i = 0; i < data.size(); i++) {
            ContactModel contactModel = data.get(i);
            if (contactModel.getBean() instanceof OnlineStatusFriendInfo) {
                OnlineStatusFriendInfo info = ((OnlineStatusFriendInfo) contactModel.getBean());
                UserOnlineStatus status = statusMap.get(info.getFriendDetail().getUserId());
                // Only update statuses actually present in statusMap, avoiding incorrectly marking
                // absent users as offline
                if (status != null) {
                    boolean online = status.isOnline();
                    if (info.isOnline() != online) {
                        info.setOnline(online);
                        headerAndFooterWrapper.notifyItemChanged(i + headersCount);
                    }
                }
            }
        }
    }

    /**
     * Sets the contact list item click listener.
     *
     * @param listener {@link OnActionClickListener}
     */
    public void setOnItemClickListener(OnActionClickListener<ContactModel> listener) {
        this.onItemClickListener = listener;
    }

    /**
     * Sets the contact list remove button click listener.
     *
     * @param listener {@link OnActionClickListener}
     */
    public void setOnItemRemoveClickListener(OnActionClickListener<ContactModel> listener) {
        this.onItemRemoveClickListener = listener;
    }

    public void addHeaderView(View view) {
        if (headerAndFooterWrapper != null) {
            headerAndFooterWrapper.addHeaderView(view);
        }
    }

    public void addFootView(View view) {
        if (headerAndFooterWrapper != null) {
            headerAndFooterWrapper.addFootView(view);
        }
    }

    public void setEnableLoadMore(boolean isEnable) {
        if (refreshLayout != null) {
            refreshLayout.setEnableLoadMore(isEnable);
        }
    }

    private void setSideBarContactLetters(String[] letters) {
        if (sideBarContact != null && showSideBar) {
            sideBarContact.setLetters(letters);
        }
    }

    public void setShowItemRemoveButton(boolean isShow) {
        if (contactListAdapter != null) {
            contactListAdapter.setShowItemRemoveButton(isShow);
        }
    }

    /**
     * Sets the paged data loader.
     *
     * @param onPageLoader the paged data loader
     */
    public void setOnPageDataLoader(OnPagedDataLoader onPageLoader) {
        this.onPagedDataLoader = onPageLoader;
    }
}
