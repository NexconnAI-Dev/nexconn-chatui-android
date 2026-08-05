package ai.nexconn.chatui.channel.subchannel;

import ai.nexconn.chat.channel.ChannelType;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class SubChannelListVMFactory extends ViewModelProvider.AndroidViewModelFactory {
    private Application mApplication;
    private ChannelType mChannelType;

    public SubChannelListVMFactory(Application application, ChannelType type) {
        super(application);
        mApplication = application;
        mChannelType = type;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SubChannelListViewModel(mApplication, mChannelType);
    }
}
