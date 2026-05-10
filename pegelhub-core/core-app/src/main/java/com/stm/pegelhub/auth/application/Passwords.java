package com.stm.pegelhub.auth.application;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Random;

/**
 * A utility class to hash passwords and check passwords vs hashed values. It uses a combination of hashing and unique
 * salt. The algorithm used is PBKDF2WithHmacSHA1 which, although not the best for hashing password (vs. bcrypt) is
 * still considered robust and <a href="https://security.stackexchange.com/a/6415/12614"> recommended by NIST </a>.
 * The hashed value has 256 bits.
 */
public class Passwords {

    private static final byte[] SALT = new byte[] {
            0x69, 0x42, 0x11, 0x54,
            0x4a, 0x1d, 0x04, 0x23,
            0x23, 0x1f, 0x19, 0x11,
            0x07, 0x0c, 0x74, 0x7a
    };
    private static final String STRING_SALT = new String(Base64.getEncoder().encode(SALT));
    private static final Random RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int SALT_LENGTH = 16;
    private static final int PASSWORD_LENGTH = 64;
    private static final int KEY_LENGTH = 256;
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase(Locale.ROOT);
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "+//";
    private static final String VALID_CHARACTERS = UPPER + LOWER + DIGITS + SYMBOLS;



    /**
     * static utility class
     */
    private Passwords() { }

    /**
     * Returns a random salt to be used to hash a password.
     *
     * @return a 16 bytes random salt as String
     */
    public static String getNextSalt() {

        //TODO: the commented code causes a "Base64 invalid character" error. Need to fix this

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        return Base64.getEncoder().encodeToString(salt);
       // return new String(salt, StandardCharsets.UTF_16);

        //return STRING_SALT;
    }

    /**
     * Returns a salted and hashed password using the provided hash.<br>
     * Note - side effect: the password is destroyed (the char[] is filled with zeros)
     *
     * @param password the password to be hashed
     * @param salt     a 16 bytes salt as String, ideally obtained with the getNextSalt method
     *
     * @return the hashed password with a pinch of salt
     */
    public static String hash(String password, String salt) {
        char[] passwordChars = password.toCharArray();
        byte[] saltBytes = Base64.getDecoder().decode(salt.getBytes());

        PBEKeySpec spec = new PBEKeySpec(passwordChars, saltBytes, ITERATIONS, KEY_LENGTH);
        Arrays.fill(passwordChars, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return new String(Base64.getEncoder().encode(skf.generateSecret(spec).getEncoded()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Returns true if the given password and salt match the hashed value, false otherwise.<br>
     * Note - side effect: the password is destroyed (the char[] is filled with zeros)
     *
     * @param password     the password to check
     * @param salt         the salt used to hash the password
     * @param expectedHash the expected hashed value of the password
     *
     * @return true if the given password and salt match the hashed value, false otherwise
     */
    public static boolean isExpectedPassword(String password, String salt, String expectedHash) {
        byte[] expectedHashBytes = Base64.getDecoder().decode(expectedHash.getBytes());
        char[] passwordChars = password.toCharArray();

        String pwdHash = hash(password, getNextSalt());
        byte[] pwdHashBytes = Base64.getEncoder().encode(pwdHash.getBytes());

        Arrays.fill(passwordChars, Character.MIN_VALUE);
        if (pwdHashBytes.length != expectedHashBytes.length) return false;
        for (int i = 0; i < pwdHashBytes.length; i++) {
            if (pwdHashBytes[i] != expectedHashBytes[i]) return false;
        }
        return true;
    }

    /**
     * Generates a random password of length 64, using letters and digits.
     *
     * @return a random password
     */
    public static String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int c = RANDOM.nextInt(62);
            if (c <= 9) {
                sb.append(c);
            } else if (c < 36) {
                sb.append((char) ('a' + c - 10));
            } else {
                sb.append((char) ('A' + c - 36));
            }
        }
        return sb.toString();
    }
}