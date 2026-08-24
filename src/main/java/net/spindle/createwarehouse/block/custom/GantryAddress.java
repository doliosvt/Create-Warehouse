package net.spindle.createwarehouse.block.custom;

import java.util.Locale;
import java.util.Optional;

/** A warehouse destination such as 12A: horizontal contact 12, vertical level A. */
public record GantryAddress(int contactNumber, int levelNumber) {
    public static final int MAX_CONTACT = 256;
    public static final int MAX_LEVEL = 256;

    public GantryAddress {
        if (contactNumber < 1 || contactNumber > MAX_CONTACT)
            throw new IllegalArgumentException("Contact number is outside the supported range");
        if (levelNumber < 1 || levelNumber > MAX_LEVEL)
            throw new IllegalArgumentException("Level number is outside the supported range");
    }

    public int contactIndex() {
        return contactNumber - 1;
    }

    public int levelIndex() {
        return levelNumber - 1;
    }

    public String value() {
        return contactNumber + formatLevel(levelNumber);
    }

    public static Optional<GantryAddress> parse(String input) {
        if (input == null)
            return Optional.empty();
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        int split = 0;
        while (split < normalized.length() && Character.isDigit(normalized.charAt(split)))
            split++;
        if (split == 0 || split == normalized.length())
            return Optional.empty();

        for (int i = split; i < normalized.length(); i++)
            if (normalized.charAt(i) < 'A' || normalized.charAt(i) > 'Z')
                return Optional.empty();

        try {
            int contact = Integer.parseInt(normalized.substring(0, split));
            int level = parseLevel(normalized.substring(split));
            if (contact < 1 || contact > MAX_CONTACT || level < 1 || level > MAX_LEVEL)
                return Optional.empty();
            return Optional.of(new GantryAddress(contact, level));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static String format(int contactIndex, int levelIndex) {
        int contact = Math.min(MAX_CONTACT - 1, Math.max(0, contactIndex)) + 1;
        int level = Math.min(MAX_LEVEL - 1, Math.max(0, levelIndex)) + 1;
        return contact + formatLevel(level);
    }

    private static int parseLevel(String letters) {
        int value = 0;
        for (int i = 0; i < letters.length(); i++) {
            int digit = letters.charAt(i) - 'A' + 1;
            if (value > (MAX_LEVEL - digit) / 26)
                return MAX_LEVEL + 1;
            value = value * 26 + digit;
        }
        return value;
    }

    private static String formatLevel(int levelNumber) {
        StringBuilder result = new StringBuilder();
        int value = levelNumber;
        while (value > 0) {
            value--;
            result.append((char) ('A' + value % 26));
            value /= 26;
        }
        return result.reverse().toString();
    }
}
