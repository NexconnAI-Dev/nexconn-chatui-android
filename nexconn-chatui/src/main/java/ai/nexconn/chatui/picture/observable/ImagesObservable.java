package ai.nexconn.chatui.picture.observable;

import ai.nexconn.chatui.picture.entity.LocalMedia;
import java.util.ArrayList;
import java.util.List;

public class ImagesObservable {
    // Preview image list
    private List<LocalMedia> previewList;

    private ImagesObservable() {
        // default implementation ignored
    }

    private static class SingletonHolder {
        static ImagesObservable sInstance = new ImagesObservable();
    }

    public static ImagesObservable getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * Store images for preview use
     *
     * @param previewList
     */
    public void savePreviewMediaData(List<LocalMedia> previewList) {
        this.previewList = previewList;
    }

    /** Read preview images */
    public List<LocalMedia> readPreviewMediaData() {
        if (previewList == null) {
            previewList = new ArrayList<>();
        }
        return previewList;
    }

    /** Clear preview images */
    public void clearPreviewMediaData() {
        if (previewList != null) {
            previewList.clear();
        }
    }
}
