package dk.bankdata.api.jaxrs.encryption;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Encryption {
    /**
     * Encryption is to be used to obfuscate sensitive data.
     *
     * <p>
     *     The cipher key has to be 128 bit or 32 characters
     *
     *     Encrypted data will be returned as Base64 encoded
     *     Decrypted data is returned as a String
     * </p>
     *
     * <code>
     *      Encryption enc = new Encryption("some-secret-key");
     *      String encrypted = enc.encrypt("sensitive-data");
     * </code>
     * <code>
     *      Encryption enc = new Encryption("some-secret-key");
     *      String decrypted = enc.decrypt(encrypted);
     * </code>
     */

    private String cipherKey;

    public Encryption(String cipherKey) {
        this.cipherKey = cipherKey;
    }

    public String encrypt(String toBeEncrypted) throws EncryptionException {
        try {
            Cipher cipher = createCipher(Cipher.ENCRYPT_MODE);
            byte[] bytes = cipher.doFinal(toBeEncrypted.getBytes());

            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            String message = "Encryption failed while encrypting " + toBeEncrypted;
            throw new EncryptionException(message, e);
        }
    }

    public String decrypt(String toBeDecrypted) throws EncryptionException {
        try {
            Cipher cipher = createCipher(Cipher.DECRYPT_MODE);
            byte[] toBeDecryptedBytes = Base64.getDecoder().decode(toBeDecrypted);
            byte[] bytes = cipher.doFinal(toBeDecryptedBytes);

            return new String(bytes);
        } catch (Exception e) {
            String message = "Decryption failed while decrypting " + toBeDecrypted;
            throw new EncryptionException(message, e);
        }
    }

    private Cipher createCipher(int encryptMode) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(cipherKey.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(encryptMode, secretKeySpec);

            return cipher;
        } catch (Exception e) {
            String message = "Failed creating cipher with cipherKey " + cipherKey;
            throw new EncryptionException(message, e);
        }
    }
}
