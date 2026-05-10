package com.stm.pegelhub.auth.application;

import java.util.UUID;

/**
 * Class, which keeps the id of the currently authenticated token.
 */
public final class AuthTokenIdHolder {

    private static final ThreadLocal<UUID> tokenStore = new ThreadLocal<>();


    /**
     * @return the currently stored auth token
     */
    public static synchronized UUID get() {
        return tokenStore.get();
    }

    /**
     * @param token the new auth token to store
     */
    public static synchronized void set(UUID token) {
        tokenStore.set(token);
    }

    /**
     * deletes the currently stored auth token
     */
    public static void clear() {
        tokenStore.remove();
    }
}
