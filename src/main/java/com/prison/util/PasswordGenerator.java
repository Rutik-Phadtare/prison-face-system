package com.prison.util;

public class PasswordGenerator {

    public static void main(String[] args) {

        String plainPassword = "rutik";

        String hashed = PasswordUtil.hashPassword(plainPassword);

        System.out.println("Hashed Password:");
        System.out.println(hashed);
    }
}
