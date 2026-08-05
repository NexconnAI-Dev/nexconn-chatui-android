package ai.nexconn.chatui.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;

/**
 * Base page interface.
 *
 * @since 5.10.4
 */
public interface BasePage {

    /**
     * Called after the module is created to build the view for use in a Fragment or Activity.
     *
     * @param context context
     * @param inflater the LayoutInflater used to inflate views in the module
     * @param container the parent ViewGroup
     * @param args optional arguments provided when instantiating the module
     * @return the created UI view
     */
    @NonNull
    View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @NonNull Bundle args);
}
