package dev.murilodcosta.url_shortener.util;

public final class Base62Encoder {

    private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = BASE62_ALPHABET.length();

    private Base62Encoder() {
        // Utility class
    }

    public static String encode(long number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Number must be positive for Base62 encoding: " + number);
        }

        StringBuilder sb = new StringBuilder();
        long current = number;

        while (current > 0) {
            int remainder = (int) (current % BASE);
            sb.append(BASE62_ALPHABET.charAt(remainder));
            current /= BASE;
        }

        return sb.reverse().toString();
    }

    public static long decode(String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("String cannot be null or empty");
        }

        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int digit = BASE62_ALPHABET.indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result = result * BASE + digit;
        }

        return result;
    }
}
