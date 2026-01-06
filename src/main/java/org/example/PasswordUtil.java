package org.example;

import java.security.MessageDigest;

public class PasswordUtil {

    private static final String SALT = "ClubNexus@2026";

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String salted = password + SALT;
            byte[] hash = md.digest(salted.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Hashing failed");
        }
    }
}
