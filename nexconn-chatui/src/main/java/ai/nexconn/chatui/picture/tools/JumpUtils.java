package ai.nexconn.chatui.picture.tools;

import ai.nexconn.chatui.picture.PicturePreviewActivity;
import ai.nexconn.chatui.picture.PictureVideoPlayActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class JumpUtils {
    public static void startPictureVideoPlayActivity(Context context, Bundle bundle) {
        if (!DoubleUtils.isFastDoubleClick()) {
            Intent intent = new Intent();
            intent.setClass(context, PictureVideoPlayActivity.class);
            intent.putExtras(bundle);
            context.startActivity(intent);
        }
    }

    public static void startPicturePreviewActivity(Context context, Bundle bundle) {
        if (!DoubleUtils.isFastDoubleClick()) {
            Intent intent = new Intent();
            intent.setClass(context, PicturePreviewActivity.class);
            intent.putExtras(bundle);
            if (context instanceof Activity) {
                ((Activity) context).startActivity(intent);
            }
        }
    }
}
