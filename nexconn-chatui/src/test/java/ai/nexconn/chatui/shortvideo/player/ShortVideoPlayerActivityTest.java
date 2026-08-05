package ai.nexconn.chatui.shortvideo.player;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ShortVideoPlayerActivityTest {
    @Test
    public void keepCurrentItemAfterHeadInsertMovesByInsertedCount() {
        assertEquals(5, ShortVideoPlayerActivity.keepCurrentItemAfterHeadInsert(2, 3));
    }

    @Test
    public void keepCurrentItemAfterHeadInsertDoesNotMoveWhenNothingInserted() {
        assertEquals(2, ShortVideoPlayerActivity.keepCurrentItemAfterHeadInsert(2, 0));
    }
}
