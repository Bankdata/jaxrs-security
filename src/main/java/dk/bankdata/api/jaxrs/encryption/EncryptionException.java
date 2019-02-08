package dk.bankdata.api.jaxrs.encryption;

public class EncryptionException extends RuntimeException {
    private String detailedMessage;

    public EncryptionException(String detailedMessage, Exception cause) {
        this.detailedMessage = detailedMessage;
        this.initCause(cause);
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }
}
