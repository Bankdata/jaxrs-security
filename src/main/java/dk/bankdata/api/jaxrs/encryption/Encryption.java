package dk.bankdata.api.jaxrs.encryption;

import dk.bankdata.api.jaxrs.environment.Environment;
import dk.bankdata.api.types.ProblemDetails;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class Encryption {
    @Inject Environment environment;

    public String encrypt(String toBeEncrypted) {
        try {
            Cipher cipher = createCipher(Cipher.ENCRYPT_MODE);
            byte[] bytes = cipher.doFinal(toBeEncrypted.getBytes());

            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Error while encrypting string")
                    .detail(e.getMessage())
                    .status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            throw new EncryptionException(builder.build(), e);
        }
    }

    public String decrypt(String toBeDecrypted) {
        try {
            Cipher cipher = createCipher(Cipher.DECRYPT_MODE);
            byte[] toBeDecryptedBytes = Base64.getDecoder().decode(toBeDecrypted);
            byte[] bytes = cipher.doFinal(toBeDecryptedBytes);

            return new String(bytes);
        } catch (Exception e) {
            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Error while decrypting string")
                    .detail(e.getMessage())
                    .status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            throw new EncryptionException(builder.build(), e);
        }
    }

    private Cipher createCipher(int encryptMode) {
        try {
            String cipherKey = environment.getCipherKey();
            SecretKeySpec secretKeySpec = new SecretKeySpec(cipherKey.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(encryptMode, secretKeySpec);

            return cipher;
        } catch (Exception e) {
            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Error while creating cipher")
                    .detail(e.getMessage())
                    .status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            throw new EncryptionException(builder.build(), e);
        }
    }
}
