package dk.bankdata.api.jaxrs.encryption;

public class EncryptionException extends RuntimeException {
    public EncryptionException(String detailedMessage, Exception cause) {
        super(detailedMessage, cause);
    }
}
