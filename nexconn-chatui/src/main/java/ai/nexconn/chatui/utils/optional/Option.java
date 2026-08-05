package ai.nexconn.chatui.utils.optional;

import ai.nexconn.chatui.utils.function.Action0;
import ai.nexconn.chatui.utils.function.Action1;
import ai.nexconn.chatui.utils.function.Func0;
import ai.nexconn.chatui.utils.function.Func1;
import java.util.concurrent.Callable;

/**
 * Lightweight Optional container replacing the RxJava-style Option/Some/None hierarchy. A null
 * value represents None; a non-null value represents Some.
 */
public final class Option<T> {

    @SuppressWarnings("rawtypes")
    public static final Option NONE = new Option<>(null);

    private final T value;

    private Option(T value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <T> Option<T> ofObj(T value) {
        return value == null ? NONE : new Option<>(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> Option<T> none() {
        return NONE;
    }

    public static <T> Option<T> tryAsOption(Callable<T> c) {
        try {
            return ofObj(c.call());
        } catch (Exception e) {
            return none();
        }
    }

    public boolean isSome() {
        return value != null;
    }

    public boolean isNone() {
        return value == null;
    }

    public Option<T> ifSome(Action1<T> action) {
        if (value != null) action.call(value);
        return this;
    }

    public Option<T> ifNone(Action0 action) {
        if (value == null) action.call();
        return this;
    }

    public <R> Option<R> map(Func1<T, R> f) {
        return value == null ? none() : ofObj(f.call(value));
    }

    public <R> Option<R> flatMap(Func1<T, Option<R>> f) {
        return value == null ? none() : f.call(value);
    }

    public T orDefault(Func0<T> def) {
        return value != null ? value : def.call();
    }

    public Option<T> filter(Func1<T, Boolean> predicate) {
        return (value != null && Boolean.TRUE.equals(predicate.call(value)))
                ? this
                : Option.<T>none();
    }

    @Override
    public String toString() {
        return value == null ? "None" : "Some(" + value + ")";
    }
}
