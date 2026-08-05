package ai.nexconn.chatui.widget.pullrefresh.constant;

/**
 * Dimension value status for determining override priority. Higher ordinal means higher priority.
 */
@SuppressWarnings("WeakerAccess")
public class DimensionStatus {

    public static final DimensionStatus DefaultUnNotify =
            new DimensionStatus(0, false); // Default value, not yet notified
    public static final DimensionStatus Default = new DimensionStatus(1, true); // Default value
    public static final DimensionStatus XmlWrapUnNotify =
            new DimensionStatus(2, false); // XML calculated, not yet notified
    public static final DimensionStatus XmlWrap = new DimensionStatus(3, true); // XML calculated
    public static final DimensionStatus XmlExactUnNotify =
            new DimensionStatus(4, false); // XML view specified, not yet notified
    public static final DimensionStatus XmlExact =
            new DimensionStatus(5, true); // XML view specified
    public static final DimensionStatus XmlLayoutUnNotify =
            new DimensionStatus(6, false); // XML layout specified, not yet notified
    public static final DimensionStatus XmlLayout =
            new DimensionStatus(7, true); // XML layout specified
    public static final DimensionStatus CodeExactUnNotify =
            new DimensionStatus(8, false); // Code specified, not yet notified
    public static final DimensionStatus CodeExact = new DimensionStatus(9, true); // Code specified
    public static final DimensionStatus DeadLockUnNotify =
            new DimensionStatus(10, false); // Dead locked, not yet notified
    public static final DimensionStatus DeadLock = new DimensionStatus(10, true); // Dead locked

    public final int ordinal;
    public final boolean notified;

    public static final DimensionStatus[] values =
            new DimensionStatus[] {
                DefaultUnNotify,
                Default,
                XmlWrapUnNotify,
                XmlWrap,
                XmlExactUnNotify,
                XmlExact,
                XmlLayoutUnNotify,
                XmlLayout,
                CodeExactUnNotify,
                CodeExact,
                DeadLockUnNotify,
                DeadLock
            };

    private DimensionStatus(int ordinal, boolean notified) {
        this.ordinal = ordinal;
        this.notified = notified;
    }

    /**
     * Convert to unnotified status.
     *
     * @return Unnotified status
     */
    public DimensionStatus unNotify() {
        if (notified) {
            DimensionStatus prev = values[ordinal - 1];
            if (!prev.notified) {
                return prev;
            }
            return DefaultUnNotify;
        }
        return this;
    }

    /**
     * Convert to notified status.
     *
     * @return Notified status
     */
    public DimensionStatus notified() {
        if (!notified) {
            return values[ordinal + 1];
        }
        return this;
    }

    /**
     * Whether this status can be replaced by a new one.
     *
     * @param status New status
     * @return true if replaceable
     */
    public boolean canReplaceWith(DimensionStatus status) {
        return ordinal < status.ordinal
                || ((!notified || CodeExact == this) && ordinal == status.ordinal);
    }

    //    /**
    //     * Whether the new status has not been reached
    //     * @param status New status
    //     * @return greater than or equal to
    //     */
    //    public boolean gteStatusWith(DimensionStatus status) {
    //        return ordinal() >= status.ordinal();
    //    }
}
