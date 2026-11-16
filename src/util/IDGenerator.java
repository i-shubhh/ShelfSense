package util;

import java.security.SecureRandom;
import java.util.Set;

/**
 * Thread-safe identifier generator using SecureRandom for high entropy.
 * Generates unique Transaction IDs in the format: SHELF-TXN-XXXXXXXX.
 */
public final class IDGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TXN_PREFIX = "SHELF-TXN-";

    private IDGenerator() {
    }

    /**
     * Generates a guaranteed unique random Transaction ID using SecureRandom.
     * Example: SHELF-TXN-8F3A92C1
     *
     * @param existingIds set of currently existing IDs to guarantee zero collisions
     * @return unique Transaction ID string
     */
    public static String generateTransactionId(Set<String> existingIds) {
        String id;
        do {
            int part1 = SECURE_RANDOM.nextInt(0x10000);
            int part2 = SECURE_RANDOM.nextInt(0x10000);
            id = String.format("%s%04X%04X", TXN_PREFIX, part1, part2);
        } while (existingIds != null && existingIds.contains(id));
        return id;
    }

    /**
     * Overloaded generator without existing set (for tests / standalone).
     */
    public static String generateTransactionId() {
        return generateTransactionId(null);
    }

    /**
     * Formats a member integer ID into standard code (e.g., M001, M042).
     */
    public static String formatMemberId(int memberId) {
        return String.format("M%03d", memberId);
    }

    /**
     * Formats an item code based on its type and id (e.g., B1001, R1002, G1003, D1004).
     */
    public static String formatItemCode(String type, int itemId) {
        if (type == null) {
            return "I" + itemId;
        }
        String prefix;
        switch (type.toUpperCase()) {
            case "BOOK":
                prefix = "B";
                break;
            case "REFERENCE":
                prefix = "R";
                break;
            case "MAGAZINE":
                prefix = "G";
                break;
            case "DIGITAL":
                prefix = "D";
                break;
            default:
                prefix = type.substring(0, 1).toUpperCase();
                break;
        }
        return prefix + itemId;
    }
}

