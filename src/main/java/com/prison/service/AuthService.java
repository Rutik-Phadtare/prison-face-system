package com.prison.service;

import com.prison.dao.UserDao;
import com.prison.model.User;
import com.prison.util.PasswordUtil;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public User authenticate(String username, String password) {

        User user = userDao.findByUsername(username);

        if (user == null) return null;

        boolean valid =
                PasswordUtil.verifyPassword(password, user.getPasswordHash());

        return valid ? user : null;
    }
}
