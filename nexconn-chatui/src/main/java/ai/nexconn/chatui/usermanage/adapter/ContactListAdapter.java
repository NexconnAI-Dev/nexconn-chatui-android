package ai.nexconn.chatui.usermanage.adapter;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.interfaces.OnActionClickListener;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.adapter.vh.ContactSelectableViewHolder;
import ai.nexconn.chatui.usermanage.adapter.vh.ContactTitleViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adapter for the contact list.
 *
 * @since 5.12.0
 */
public class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ContactModel> data = new ArrayList<>();
    private OnActionClickListener<ContactModel> onItemClickListener;
    private OnActionClickListener<ContactModel> onItemRemoveClickListener;
    private final boolean showSelectButton;
    private final boolean showItemRightArrow;
    private final boolean showItemRightText;
    private final boolean showItemSelectAutoUpdate;
    private boolean showItemRemoveButton;

    public ContactListAdapter(
            boolean showSelectButton,
            boolean showItemRightArrow,
            boolean showItemRightText,
            boolean showItemSelectAutoUpdate,
            boolean showItemRemoveButton) {
        this.showSelectButton = showSelectButton;
        this.showItemRightArrow = showItemRightArrow;
        this.showItemRightText = showItemRightText;
        this.showItemSelectAutoUpdate = showItemSelectAutoUpdate;
        this.showItemRemoveButton = showItemRemoveButton;
    }

    public void setOnItemClickListener(OnActionClickListener<ContactModel> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnItemRemoveClickListener(OnActionClickListener<ContactModel> listener) {
        onItemRemoveClickListener = listener;
    }

    public void setData(List<ContactModel> newData) {
        if (newData == null) {
            newData = new CopyOnWriteArrayList<>();
        }
        this.data = new CopyOnWriteArrayList<>(newData);
        notifyDataSetChanged();
    }

    public List<ContactModel> getData() {
        return data;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(viewType, parent, false);

        if (viewType == R.layout.nc_item_contact_selectable) {
            return new ContactSelectableViewHolder(
                    itemView,
                    onItemClickListener,
                    onItemRemoveClickListener,
                    showSelectButton,
                    showItemRightArrow,
                    showItemRightText,
                    showItemSelectAutoUpdate,
                    showItemRemoveButton);
        } else if (viewType == R.layout.nc_item_contact_title) {
            return new ContactTitleViewHolder(itemView);
        } else {
            throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ContactModel contactModel = data.get(position);
        if (holder instanceof ContactSelectableViewHolder) {
            ((ContactSelectableViewHolder) holder).setShowItemRemoveButton(showItemRemoveButton);
            // Determine whether to hide divider: 1. last item 2. next item is a title
            boolean showDivider = true;
            if (position == data.size() - 1) {
                // Last item, hide divider
                showDivider = false;
            } else {
                ContactModel<?> nextModel = data.get(position + 1);
                if (nextModel.getContactType() == ContactModel.ItemType.TITLE) {
                    // Next item is a title, hide divider
                    showDivider = false;
                }
            }
            ((ContactSelectableViewHolder) holder).setDividerVisibility(showDivider);
            ((ContactSelectableViewHolder) holder).bind(contactModel);
        } else if (holder instanceof ContactTitleViewHolder) {
            ((ContactTitleViewHolder) holder).bind(contactModel);
        }
    }

    @Override
    public int getItemViewType(int position) {
        ContactModel<?> contactModel = data.get(position);
        return contactModel.getContactType() == ContactModel.ItemType.CONTENT
                ? R.layout.nc_item_contact_selectable
                : R.layout.nc_item_contact_title;
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public int getPositionForSection(char section) {
        for (int i = 0; i < data.size(); i++) {
            ContactModel<?> contactModel = data.get(i);
            if (contactModel.getContactType() == ContactModel.ItemType.TITLE) {
                if (((String) contactModel.getBean()).charAt(0) == section) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void setShowItemRemoveButton(boolean showItemRemoveButton) {
        this.showItemRemoveButton = showItemRemoveButton;
        notifyDataSetChanged();
    }
}
