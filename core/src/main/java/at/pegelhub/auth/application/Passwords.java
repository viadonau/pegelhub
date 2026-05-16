package at.pegelhub.auth.application;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
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

    private static final Random RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int SALT_LENGTH = 16;
    private static final int PASSWORD_LENGTH = 64;
    private static final int KEY_LENGTH = 256;
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase(Locale.ROOT);
    private static final String DIGITS = "0123456789";
    private static final String VALID_CHARACTERS = UPPER + LOWER + DIGITS;



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
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        return Base64.getEncoder().encodeToString(salt);
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
            return Base64.getEncoder().encodeToString(skf.generateSecret(spec).getEncoded());
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
        byte[] expectedHashBytes = Base64.getDecoder().decode(expectedHash);
        byte[] actualHashBytes = Base64.getDecoder().decode(hash(password, salt));
        return MessageDigest.isEqual(actualHashBytes, expectedHashBytes);
    }

    /**
     * Generates a random password of length 64, using letters and digits.
     *
     * @return a random password
     */
    public static String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int c = RANDOM.nextInt(VALID_CHARACTERS.length());
            sb.append(VALID_CHARACTERS.charAt(c));
        }
        return sb.toString();
    }
}
