package SMTP_Email_Simulator;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * CryptoUtil: AES encrypt/decrypt using key derived from passphrase.
 * Keep the PASSPHRASE identical across Server and Client (this file).
 */
public class CryptoUtil {
    private static final String PASSPHRASE = "ChangeThisPassphraseToSomethingStrong!";
    private static final String ALGO = "AES";
    private static final SecretKeySpec KEY_SPEC = createKeySpec();

    private static SecretKeySpec createKeySpec() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(PASSPHRASE.getBytes(StandardCharsets.UTF_8));
            byte[] key16 = new byte[16];
            System.arraycopy(key, 0, key16, 0, 16);
            return new SecretKeySpec(key16, ALGO);
        } catch (Exception ex) {
            throw new RuntimeException("Error creating AES key", ex);
        }
    }

    public static String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, KEY_SPEC);
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(enc);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String cipherText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, KEY_SPEC);
            byte[] original = cipher.doFinal(decoded);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "[decrypt error]";
        }
    }
}
