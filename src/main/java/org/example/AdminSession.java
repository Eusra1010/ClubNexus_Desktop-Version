package org.example;

public class AdminSession {

    private static String adminId;
    private static String clubName;

    public static void setAdmin(String id, String club) {
        adminId = id;
        clubName = club;
    }

    public static String getAdminId() {
        return adminId;
    }

    public static String getClubName() {
        return clubName;
    }

    public static void clear() {
        adminId = null;
        clubName = null;
    }
}
