package dk.bankdata.api.jaxrs.encryption;

import dk.bankdata.api.types.ProblemDetails;

import java.util.Objects;

public class EncryptionException extends RuntimeException {
    private ProblemDetails problemDetails;

    public EncryptionException(ProblemDetails problemDetails, Exception cause) {
        this.problemDetails = problemDetails;
        this.initCause(cause);
    }

    public ProblemDetails getProblemDetails() {
        return problemDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptionException that = (EncryptionException) o;
        return Objects.equals(problemDetails, that.problemDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(problemDetails);
    }

    @Override
    public String toString() {
        return "EncryptionException{" +
                "problemDetails=" + problemDetails +
                '}';
    }

}
