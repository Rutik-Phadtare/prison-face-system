package com.prison.session;

import com.prison.model.User;

public class UserSession {

    private static User currentUser;

    private UserSession() {}

    public static void setUser(User user) {
        currentUser = user;
    }

    public static User getUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
