package ai.nexconn.chatui.shortvideo.record;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.activity.NCBaseNoActionbarActivity;
import ai.nexconn.chatui.utils.file.ChatUIStorageUtils;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.WindowManager;
import java.io.File;

public class ShortVideoRecordActivity extends NCBaseNoActionbarActivity {
    public static final String TAG = "Sight-ShortVideoRecordActivity";
    private CameraView mCameraView;
    private static final int DEFAULT_MAX_RECORD_DURATION = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getWindow() != null) {
            int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setFlags(flag, flag);
        }
        setContentView(R.layout.nc_activity_sight_record);

        mCameraView = findViewById(R.id.cameraView);
        mCameraView.setAutoFocus(false);
        mCameraView.setSupportCapture(getIntent().getBooleanExtra("supportCapture", false));
        mCameraView.setSaveVideoPath(getIntent().getStringExtra("recordSightDir"));
        int maxRecordDuration =
                getIntent().getIntExtra("maxRecordDuration", DEFAULT_MAX_RECORD_DURATION);
        if (maxRecordDuration <= 0) {
            maxRecordDuration = DEFAULT_MAX_RECORD_DURATION;
        }
        mCameraView.setMaxRecordDuration(maxRecordDuration);
        mCameraView.setCameraViewListener(
                new CameraView.CameraViewListener() {
                    @Override
                    public void quit() {
                        // default implementation ignored
                    }

                    @Override
                    public void captureSuccess(Bitmap bitmap) {
                        // default implementation ignored
                    }

                    @Override
                    public void recordSuccess(String url, int recordTime) {
                        if (TextUtils.isEmpty(url)) {
                            setResult(RESULT_CANCELED);
                            ShortVideoRecordActivity.this.finish();
                            return;
                        }
                        File file = new File(url);
                        if (!file.exists()) {
                            setResult(RESULT_CANCELED);
                            ShortVideoRecordActivity.this.finish();
                            return;
                        }
                        boolean result =
                                ChatUIStorageUtils.saveMediaToPublicDir(
                                        ShortVideoRecordActivity.this,
                                        file,
                                        ChatUIStorageUtils.MediaType.VIDEO);
                        RLog.i(TAG, "RecordSuccess save result" + result);
                        Intent intent = new Intent();
                        intent.putExtra("recordSightUrl", url);
                        intent.putExtra("recordSightTime", recordTime);
                        setResult(RESULT_OK, intent);
                        ShortVideoRecordActivity.this.finish();
                    }

                    @Override
                    public void finish() {
                        ShortVideoRecordActivity.this.finish();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.onPause();
    }

    @Override
    public void finish() {
        super.finish();
        // When a fullscreen Activity finishes and returns to a non-fullscreen one, it causes page
        // redraw flicker
        // (typical symptom: RecyclerView scrolls down slightly). Clear the fullscreen flag after
        // finish to avoid this.
        if (getWindow() != null) {
            int flag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
            getWindow().setFlags(flag, flag);
        }
    }
}
