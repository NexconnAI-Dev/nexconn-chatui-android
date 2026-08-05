package ai.nexconn.chatui.channel.event.page;

/** Input bar refresh event. */
public class InputBarEvent implements PageEvent {
    public Type mType;
    public String mExtra;

    public InputBarEvent(Type type, String mExtra) {
        this.mType = type;
        this.mExtra = mExtra;
    }

    public enum Type {
        ReEdit,
        ShowMoreMenu,
        HideMoreMenu,
        ActiveMoreMenu,
        InactiveMoreMenu
    }
}
