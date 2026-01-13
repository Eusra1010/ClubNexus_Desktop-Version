package org.example;

public class StudentSession {
    private static String currentRoll;

    public static void setCurrentRoll(String roll) {
        currentRoll = roll;
    }

    public static String getCurrentRoll() {
        return currentRoll;
    }

    public static boolean isLoggedIn() {
        return currentRoll != null && !currentRoll.isBlank();
    }
}
