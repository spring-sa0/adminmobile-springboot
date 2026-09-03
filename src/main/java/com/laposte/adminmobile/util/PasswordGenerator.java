package com.laposte.adminmobile.util;

import java.security.SecureRandom;

/**
 * Port of the legacy {@code admin.mobile.dao.PasswordGenerator} and
 * {@code admin.mobile.dao.generePassword} classes.
 * <p>
 * Both legacy classes contained the exact same character-picking algorithm (lower-case,
 * upper-case, digit, randomly chosen 8-10 characters long) - {@code PasswordGenerator} exposed it
 * as an instance method {@code GenPassword()} returning the password, {@code generePassword} only
 * had a {@code main(String[])} that printed it to stdout via an identical copy of the loop, and
 * was evidently a throwaway/manual-test entry point rather than a class anything else called.
 * They are merged here into a single stateless utility.
 * <p>
 * Two deliberate changes from the legacy code: this uses {@link SecureRandom} instead of the
 * shared, non-thread-safe {@code static Random} field the originals used (a generator used to
 * mint account credentials should not use a predictable PRNG nor mutable shared state), and the
 * commented-out "special characters" branch ({@code Zeta}) - dead in both originals - is dropped
 * rather than carried forward as inert code. The character pools, target length (8, 9 or 10
 * chars, uniformly picked among the three), and the letters/digits themselves are reproduced
 * exactly.
 */
public final class PasswordGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "1234567890";

    private PasswordGenerator() {
    }

    /**
     * Generates a random 8, 9 or 10 character password mixing lower-case letters, upper-case
     * letters and digits, matching the algorithm of the legacy
     * {@code PasswordGenerator.GenPassword()} / {@code generePassword.main()}.
     */
    public static String generatePassword() {
        StringBuilder password = new StringBuilder();
        while (password.length() != 8 && password.length() != 9 && password.length() != 10) {
            int pick = RANDOM.nextInt(3);
            if (pick == 0) {
                password.append(LOWER.charAt(RANDOM.nextInt(25)));
            } else if (pick == 1) {
                password.append(DIGITS.charAt(RANDOM.nextInt(9)));
            } else {
                password.append(UPPER.charAt(RANDOM.nextInt(25)));
            }
        }
        return password.toString();
    }
}
