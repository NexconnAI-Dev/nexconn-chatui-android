package ai.nexconn.chatui.activity;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.picture.tools.ToastUtils;
import ai.nexconn.chatui.utils.common.ChatUIUtils;
import ai.nexconn.chatui.utils.language.NCConfigurationManager;
import ai.nexconn.chatui.utils.permission.PermissionCheckUtil;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.utils.view.StatusBarUtil;
import ai.nexconn.chatui.widget.TitleBar;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class NCBaseActivity extends AppCompatActivity {
    protected ViewFlipper mContentView;
    protected TitleBar mTitleBar;

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = NCConfigurationManager.getInstance().getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ChatUIUtils.fixAndroid8ActivityCrash(this);
        super.onCreate(savedInstanceState);
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        super.setContentView(R.layout.nc_base_activity_layout);
        mTitleBar = findViewById(R.id.nc_title_bar);
        mTitleBar.setOnBackClickListener(
                new TitleBar.OnBackClickListener() {
                    @Override
                    public void onBackClick() {
                        finish();
                    }
                });
        mTitleBar.setBackgroundColor(
                ChatUIThemeManager.getColorFromAttrId(this, R.attr.nc_common_background_color));
        mContentView = findViewById(R.id.nc_base_container);
    }

    @Override
    public void setContentView(int resId) {
        View view = LayoutInflater.from(this).inflate(resId, null);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1);
        mContentView.addView(view, lp);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissionResultIncompatible(permissions, grantResults)) {
            ToastUtils.s(this, getString(R.string.nc_permission_request_failed));
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void initStatusBar(int colorResId) {
        StatusBarUtil.setRootViewFitsSystemWindows(this, true);
        StatusBarUtil.setStatusBarColor(
                this,
                colorResId == 0
                        ? getResources()
                                .getColor(
                                        ChatUIThemeManager.getAttrResId(
                                                this, R.attr.nc_primary_color))
                        : getResources().getColor(colorResId)); // Color.parseColor("#F5F6F9")
    }

    /** Extracts ChannelIdentifier from Intent, or constructs one from ChannelType and targetId. */
    public ChannelIdentifier initConversationIdentifier() {
        Intent intent = getIntent();
        if (intent.hasExtra(RouteUtils.CHANNEL_IDENTIFIER)) {
            ChannelIdentifier identifier = intent.getParcelableExtra(RouteUtils.CHANNEL_IDENTIFIER);
            if (identifier != null) {
                return identifier;
            }
        }

        String type = intent.getStringExtra(RouteUtils.CHANNEL_TYPE);
        ChannelType channelType = ChannelType.valueOf(type.toUpperCase(Locale.US));
        String targetId = intent.getStringExtra(RouteUtils.TARGET_ID);
        return new ChannelIdentifier(channelType, targetId);
    }
}
