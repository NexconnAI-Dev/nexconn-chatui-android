package ai.nexconn.chatui.widget;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.model.MessageDeletedEvent;
import ai.nexconn.chat.message.model.ReferenceMessageStatus;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.picture.widget.BaseDialogFragment;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.utils.text.TextViewUtils;
import ai.nexconn.chatui.widget.dialog.OptionsPopupDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;

public class ReferenceDialog extends BaseDialogFragment {
    private TextView referenceShowText;
    private UiMessage mUiMessage;

    private String mRecallHandlerId;
    private final MessageHandler mRecallHandler =
            new MessageHandler() {
                @Override
                public void onMessageDeleted(@NonNull MessageDeletedEvent event) {
                    if (mUiMessage == null || event.getMessages() == null) return;
                    for (Message msg : event.getMessages()) {
                        if (msg != null
                                && mUiMessage.getMessage().getClientId() == msg.getClientId()) {
                            dismiss();
                            return;
                        }
                    }
                }
            };

    public ReferenceDialog(UiMessage uiMessage) {
        this.mUiMessage = uiMessage;
    }

    @Override
    protected void findView() {
        referenceShowText = mRootView.findViewById(R.id.nc_reference_window_text);
        referenceShowText.setOnLongClickListener(
                view -> {
                    showCopyDialog();
                    return false;
                });
        referenceShowText.setMovementMethod(
                new LinkTextViewMovementMethod(
                        new ILinkClickListener() {
                            @Override
                            public boolean onLinkClick(String link) {
                                String str = link.toLowerCase();
                                if (str.startsWith("http") || str.startsWith("https")) {
                                    RouteUtils.routeToWebActivity(getContext(), link);
                                    return true;
                                }

                                return false;
                            }
                        }));
    }

    @Override
    protected void initView() {
        mRootView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mRecallHandlerId = "ReferenceDialog_recall_" + System.identityHashCode(this);
        NCEngine.addMessageHandler(mRecallHandlerId, mRecallHandler);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mRecallHandlerId != null) {
            NCEngine.removeMessageHandler(mRecallHandlerId);
            mRecallHandlerId = null;
        }
    }

    @Override
    public void bindData() {
        CharSequence referenceText =
                mUiMessage.getReferenceContentSpannable() != null
                        ? mUiMessage.getReferenceContentSpannable().toString()
                        : "";
        SpannableStringBuilder spannable =
                TextViewUtils.getSpannable(referenceText.toString(), this::setText);
        setText(spannable);
    }

    protected void setText(SpannableStringBuilder span) {
        ReferenceMessage content = (ReferenceMessage) mUiMessage.getMessage().getContent();
        ReferenceMessageStatus referMsgStatus = content.getReferMsgStatus();
        if (referMsgStatus == ReferenceMessageStatus.MODIFIED) {
            SpannableStringBuilder contentSpannable = new SpannableStringBuilder(span);
            String text = getString(R.string.nc_edit_status_success);
            SpannableStringBuilder spannable = new SpannableStringBuilder("（" + text + "）");
            ForegroundColorSpan colorSpan =
                    new ForegroundColorSpan(
                            getResources().getColor(R.color.nc_edit_success_status));
            spannable.setSpan(colorSpan, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            contentSpannable.append(spannable);
            referenceShowText.setText(contentSpannable);
        } else if (referMsgStatus == ReferenceMessageStatus.DELETED
                || referMsgStatus == ReferenceMessageStatus.RECALLED) {
            referenceShowText.setText(getString(R.string.nc_reference_status_delete));
        } else {
            // Fallback for audited/filtered reference content:
            // backend may return DEFAULT status with empty body.
            if (TextUtils.isEmpty(span)) {
                referenceShowText.setText(getString(R.string.nc_reference_status_delete));
            } else {
                referenceShowText.setText(span);
            }
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.nc_reference_popupwindow;
    }

    @Override
    protected float getScreenWidthProportion() {
        return 1f;
    }

    @Override
    protected int getScreenHeightProportion() {
        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    @Override
    protected int getBackgroundDrawableRes() {
        return R.color.app_color_white;
    }

    private void showCopyDialog() {
        if (getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing()) {
            return;
        }
        String[] items = new String[] {getString(R.string.nc_dialog_item_message_copy)};
        OptionsPopupDialog.newInstance(getActivity(), items)
                .setOptionsPopupDialogListener(
                        new OptionsPopupDialog.OnOptionsItemClickedListener() {
                            @Override
                            public void onOptionsItemClicked(int which) {
                                if (which == 0) {
                                    CharSequence text =
                                            mUiMessage.getReferenceContentSpannable() != null
                                                    ? mUiMessage.getReferenceContentSpannable()
                                                    : "";
                                    copyText(text.toString());
                                }
                            }
                        })
                .show();
    }

    private void copyText(String text) {
        if (getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing()) {
            return;
        }
        ClipboardManager clipboard =
                (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            try {
                clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
            } catch (Exception e) {
            }
        }
    }
}
