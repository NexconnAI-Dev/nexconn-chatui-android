package ai.nexconn.chatui.activity;

import android.os.Bundle;

public class CombinePicturePagerActivity extends PicturePagerActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPicturePager(ai.nexconn.chatui.utils.message.MessageHolder.takeMessage());
    }

    @Override
    protected boolean shouldInitOnCreate() {
        return false;
    }

    @Override
    protected boolean enableAdjacentImageFetch() {
        return false;
    }
}
