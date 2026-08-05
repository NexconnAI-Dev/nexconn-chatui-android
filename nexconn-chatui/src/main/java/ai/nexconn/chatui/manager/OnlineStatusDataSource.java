package ai.nexconn.chatui.manager;

import java.util.List;

/**
 * Online status subscription data source interface
 *
 * @since 5.32.0
 */
public interface OnlineStatusDataSource {

    /**
     * Returns a user list sorted by priority (lower position = lower priority). Notes:
     *
     * <p>1. When the total subscriptions exceed the limit, the first
     * OnLineStatusManager#DEFAULT_SUBSCRIBE_NUMBER users are kept; users not in the list but
     * already subscribed will be unsubscribed, and users in the list but not yet subscribed will be
     * subscribed.
     *
     * <p>2. To prevent a user ID from being evicted when the limit is exceeded, place it near the
     * front of the list.
     *
     * <p>3. The SDK caches the last returned data; call OnLineStatusManager#clearPriorityUserList
     * to clear the cache.
     *
     * @return user ID list
     */
    List<String> onPriorityUserList();
}
