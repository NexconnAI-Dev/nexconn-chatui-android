package ai.nexconn.chatui.widget.pullrefresh.constant;

/** Transform style for header/footer components during drag. */
@SuppressWarnings("DeprecatedIsStillUsed")
public class SpinnerStyle {

    public static final SpinnerStyle Translate = new SpinnerStyle(0, true, false);

    /**
     * Scale mode dynamically measures the header and performs layout during pull-down, reducing app
     * performance. Built-in Headers have migrated from Scale to FixedBehind for better performance.
     *
     * @deprecated use {@link SpinnerStyle#FixedBehind}
     */
    @Deprecated public static final SpinnerStyle Scale = new SpinnerStyle(1, true, true);

    public static final SpinnerStyle FixedBehind = new SpinnerStyle(2, false, false);
    public static final SpinnerStyle FixedFront = new SpinnerStyle(3, true, false);
    public static final SpinnerStyle MatchLayout = new SpinnerStyle(4, true, false);

    public static final SpinnerStyle[] values =
            new SpinnerStyle[] {
                Translate, // Translate: HeaderView height does not change
                Scale, // Scale: Triggers OnDraw when pulling down and bouncing (HeaderView height
                // changes)
                FixedBehind, // Fixed behind: HeaderView height does not change
                FixedFront, // Fixed in front: HeaderView height does not change
                MatchLayout // Match layout: HeaderView height does not change, fills entire
                // RefreshLayout
            };

    public final int ordinal;
    public final boolean front;
    public final boolean scale;

    protected SpinnerStyle(int ordinal, boolean front, boolean scale) {
        this.ordinal = ordinal;
        this.front = front;
        this.scale = scale;
    }
}
