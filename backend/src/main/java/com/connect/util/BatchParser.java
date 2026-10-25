package com.connect.util;

/**
 * Derives a student's batch (admission year) from their college email.
 *
 * <p>VIT-AP student emails start with the registration number, whose leading
 * two digits are the admission year — e.g. {@code 26mic7142@vitapstudent.ac.in}
 * has batch {@code 26}.
 */
public final class BatchParser {

    private BatchParser() {
    }

    /**
     * @return the leading numeric run of the email's local part as an int, or
     *         {@code null} if the email is null or does not start with a digit
     */
    public static Integer parseBatch(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        String local = at >= 0 ? email.substring(0, at) : email;

        int end = 0;
        while (end < local.length() && Character.isDigit(local.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return null;
        }
        try {
            return Integer.parseInt(local.substring(0, end));
        } catch (NumberFormatException e) {
            return null; // absurdly long numeric prefix
        }
    }
}
