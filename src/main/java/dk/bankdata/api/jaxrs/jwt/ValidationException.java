package dk.bankdata.api.jaxrs.jwt;

import dk.bankdata.api.types.ErrorDetails;

import java.util.Objects;

public class ValidationException extends RuntimeException {
    private ErrorDetails errorDetails;

    public ValidationException(ErrorDetails errorDetails, Exception cause) {
        this.errorDetails = errorDetails;
        this.initCause(cause);
    }

    public ErrorDetails getProblemDetails() {
        return errorDetails;
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorDetails);
    }

    @Override
    public String toString() {
        return "ValidationException{" +
                "problemDetails=" + errorDetails +
                '}';
    }
}
