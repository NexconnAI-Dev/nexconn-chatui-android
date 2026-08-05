package ai.nexconn.chatui.base;

import ai.nexconn.chat.error.NCError;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.channel.event.page.ErrorEvent;
import android.os.Bundle;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Base ViewModel providing error event LiveData.
 *
 * @since 5.10.4
 */
public class BaseViewModel extends ViewModel {

    private final Bundle mArguments;

    public BaseViewModel(Bundle bundle) {
        this.mArguments = bundle;
    }

    protected final Bundle getArguments() {
        return this.mArguments;
    }

    protected final MutableLiveData<ErrorEvent<NCError>> errorEventLiveData =
            new MutableLiveData<>();

    /**
     * Returns the error event LiveData for observing error states.
     *
     * @return MutableLiveData<ErrorEvent<NCError>>
     */
    public MutableLiveData<ErrorEvent<NCError>> getErrorEventLiveData() {
        return errorEventLiveData;
    }

    /**
     * Posts a page-level error event.
     *
     * @param errorEvent the error event
     */
    protected void postErrorEvent(ErrorEvent<NCError> errorEvent) {
        errorEventLiveData.postValue(errorEvent);
    }

    /**
     * Safe data handler that processes data changes and errors.
     *
     * @param <T> the data type
     */
    protected abstract class SafeDataHandler<T> implements OnDataChangeListener<T> {

        @Override
        public void onDataError(NCError error) {
            postErrorEvent(ErrorEvent.obtain(error, error != null ? error.getMessage() : ""));
        }
    }
}
