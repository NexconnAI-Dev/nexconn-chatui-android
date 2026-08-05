package ai.nexconn.chatui.model;

/**
 * Base bean for DiffUtils-based partial list updates. Subclasses call {@link #change()} when
 * properties are modified so DiffUtils can detect changes via the {@code isChange} flag.
 */
public class UiBaseBean {

    /** Must be set when data is updated */
    private boolean isChange;

    public boolean isChange() {
        return isChange;
    }

    public void change() {
        isChange = true;
    }

    public void setChange(boolean change) {
        this.isChange = change;
    }
}
