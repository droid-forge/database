package promise.db;


import androidx.annotation.Nullable;

import promise.commons.model.List;


/**
 * Created on 6/27/18 by yoctopus.
 */
public interface Extras<T> {
    @Nullable
    T first();
    @Nullable T last();
    List<? extends T> all();
    List<? extends T> limit(int limit);
    <X> List<? extends T> where(X... x);
}
