package ai.nexconn.chatui.channel.feature.reference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chatui.model.UiMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link ReferenceManager#isReferencedMessageEdited}.
 *
 * <p>Verifies that outgoing reference messages use the latest model from the authoritative message
 * list to determine whether the referenced message was edited, avoiding stale snapshot state.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class ReferenceManagerEditedDetectionTest {

    /** Creates a UiMessage with messageId and hasChanged injected into internal setters. */
    private static UiMessage uiMessage(String messageId, boolean hasChanged) {
        Message message =
                new Message(
                        new ChannelIdentifier(ChannelType.DIRECT, "target"), new TextMessage("x"));
        message.setSenderUserId("sender");
        UiMessage uiMessage = new UiMessage(message);
        setField(message, "messageId", messageId);
        setField(message, "hasChanged", hasChanged);
        return uiMessage;
    }

    /** Writes a Message internal-set field through reflection. */
    private static void setField(Message target, String name, Object value) {
        try {
            java.lang.reflect.Field field = Message.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns true from the latest edited model in the list, ignoring a stale snapshot. */
    @Test
    public void usesLatestModelWhenEdited() {
        List<UiMessage> messages = Collections.singletonList(uiMessage("refer-id", true));
        assertTrue(ReferenceManager.isReferencedMessageEdited("refer-id", false, messages));
    }

    /** Returns false from the latest unedited model in the list, ignoring a stale snapshot. */
    @Test
    public void usesLatestModelWhenNotEdited() {
        List<UiMessage> messages = Collections.singletonList(uiMessage("refer-id", false));
        assertFalse(ReferenceManager.isReferencedMessageEdited("refer-id", true, messages));
    }

    /** Falls back to the snapshot hasChanged value when the referenced message is absent. */
    @Test
    public void fallsBackWhenMessageMissing() {
        List<UiMessage> messages = Collections.singletonList(uiMessage("other-id", false));
        assertTrue(ReferenceManager.isReferencedMessageEdited("refer-id", true, messages));
    }

    /** Falls back to the snapshot hasChanged value when referMsgId or the list is absent. */
    @Test
    public void fallsBackWhenIdEmptyOrListNull() {
        assertTrue(ReferenceManager.isReferencedMessageEdited("", true, new ArrayList<>()));
        assertFalse(ReferenceManager.isReferencedMessageEdited(null, false, null));
    }
}
