package com.prison.session;

import com.prison.model.User;

public class UserSession {

    private static User   currentUser;
    private static int    loginLogId = -1;   // ← NEW: tracks co_admin_login_logs row

    private UserSession() {}

    public static void setUser(User user)      { currentUser = user; }
    public static User getUser()               { return currentUser; }

    public static void setLoginLogId(int id)   { loginLogId = id; }   // ← NEW
    public static int  getLoginLogId()         { return loginLogId; }  // ← NEW

    public static void clear() {
        currentUser = null;
        loginLogId  = -1;
    }
}