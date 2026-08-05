package ai.nexconn.chatui.usermanage.adapter.vh;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.model.ContactModel;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ContactTitleViewHolder extends RecyclerView.ViewHolder {

    private final TextView catalogLetterTextView;

    public ContactTitleViewHolder(@NonNull View itemView) {
        super(itemView);
        catalogLetterTextView = itemView.findViewById(R.id.tv_catalog_letter);
    }

    public void bind(ContactModel<String> characterTitleInfoContactModel) {
        catalogLetterTextView.setText(characterTitleInfoContactModel.getBean());
    }
}
