package ai.nexconn.chatui.activity;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.picture.tools.ToastUtils;
import ai.nexconn.chatui.utils.language.NCConfigurationManager;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

public class NCBaseNoActionbarActivity extends FragmentActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = NCConfigurationManager.getInstance().getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (PermissionCheckUtil.checkPermissionResultIncompatible(permissions, grantResults)) {
            ToastUtils.s(this, getString(R.string.nc_permission_request_failed));
            return;
        }

        if (!PermissionCheckUtil.checkPermissions(this, permissions)) {
            PermissionCheckUtil.showRequestPermissionFailedAlter(this, permissions, grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
