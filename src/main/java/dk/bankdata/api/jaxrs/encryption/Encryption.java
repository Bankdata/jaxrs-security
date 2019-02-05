package dk.bankdata.api.jaxrs.encryption;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import dk.bankdata.api.types.ProblemDetails;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/*
To @Inject this class you'll have to use this method:
@Inject
@CipherKey(value = "some-cipher-key")
Encryption encryption;
 */

@ApplicationScoped
public class Encryption {
    private String cipherKey;

    public Encryption() {}

    public Encryption(String cipherKey) {
        this.cipherKey = cipherKey;
    }

    private String initCipherKey(InjectionPoint injectionPoint) {
        for (Annotation annotation : injectionPoint.getQualifiers()) {
            if (annotation.annotationType().equals(CipherKey.class)) {
                return ((CipherKey) annotation).value();
            }
        }

        throw new IllegalStateException("No @CipherKey on InjectionPoint");
    }

    @Produces
    @CipherKey
    public Encryption createEncryption(InjectionPoint injectionPoint) {
        String cipherKey = initCipherKey(injectionPoint);
        return new Encryption(cipherKey);
    }

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
            SecretKeySpec secretKeySpec = new SecretKeySpec(cipherKey.getBytes("UTF-8"), "AES");

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

    @Qualifier
    @Target({ TYPE, METHOD, PARAMETER, FIELD })
    @Retention(RUNTIME)
    @Documented
    public @interface CipherKey {
        @Nonbinding
        String value() default "";
    }
}
