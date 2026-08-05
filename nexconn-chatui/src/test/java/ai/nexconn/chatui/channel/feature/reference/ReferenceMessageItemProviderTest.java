package ai.nexconn.chatui.channel.feature.reference;

import static org.junit.Assert.assertEquals;

import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.model.ReferenceMessageStatus;
import ai.nexconn.chatui.channel.messagelist.provider.BaseMessageItemProvider;
import ai.nexconn.chatui.model.UiMessage;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 33)
public class ReferenceMessageItemProviderTest {

    @Test
    public void modifiedReferenceContentUsesIosEditedReferenceColor() {
        SpannableStringBuilder original = new SpannableStringBuilder("original message");
        int iosLightEditedReferenceColor = Color.rgb(0xA0, 0xA5, 0xAB);

        Spanned text =
                new TestProvider()
                        .buildModifiedReferenceContent(
                                original, "edited", iosLightEditedReferenceColor);

        ForegroundColorSpan[] spans =
                text.getSpans(0, text.length(), ForegroundColorSpan.class);

        assertEquals(1, spans.length);
        assertEquals(iosLightEditedReferenceColor, spans[0].getForegroundColor());
        assertEquals(0, text.getSpanStart(spans[0]));
        assertEquals(text.length(), text.getSpanEnd(spans[0]));
    }

    private static final class TestProvider
            extends BaseMessageItemProvider<ai.nexconn.chat.message.ReferenceMessage> {
        Spanned buildModifiedReferenceContent(
                SpannableStringBuilder span, String editedText, int color) {
            return buildModifiedReferenceMessageContent(span, editedText, color);
        }

        @Override
        protected ai.nexconn.chatui.widget.adapter.ViewHolder onCreateMessageContentViewHolder(
                android.view.ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        protected void bindMessageContentViewHolder(
                ai.nexconn.chatui.widget.adapter.ViewHolder holder,
                ai.nexconn.chatui.widget.adapter.ViewHolder parentHolder,
                ai.nexconn.chat.message.ReferenceMessage referenceMessage,
                UiMessage uiMessage,
                int position,
                java.util.List<UiMessage> list,
                ai.nexconn.chatui.widget.adapter.IViewProviderListener<UiMessage> listener) {}

        @Override
        protected boolean isMessageViewType(MessageContent messageContent) {
            return messageContent instanceof ai.nexconn.chat.message.ReferenceMessage;
        }

        @Override
        protected boolean onItemClick(
                ai.nexconn.chatui.widget.adapter.ViewHolder holder,
                ai.nexconn.chat.message.ReferenceMessage referenceMessage,
                UiMessage uiMessage,
                int position,
                java.util.List<UiMessage> list,
                ai.nexconn.chatui.widget.adapter.IViewProviderListener<UiMessage> listener) {
            return false;
        }

        @Override
        public SpannableStringBuilder getSummarySpannable(
                android.content.Context context,
                ai.nexconn.chat.message.ReferenceMessage referenceMessage) {
            return null;
        }
    }
}
