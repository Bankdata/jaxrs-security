package dk.bankdata.api.jaxrs.jwt;

import dk.bankdata.api.types.ProblemDetails;

import java.util.Objects;

public class ValidationException extends RuntimeException {
    private ProblemDetails problemDetails;

    public ValidationException(ProblemDetails problemDetails, Exception cause) {
        this.problemDetails = problemDetails;
        this.initCause(cause);
    }

    public ProblemDetails getProblemDetails() {
        return problemDetails;
    }

    @Override
    public int hashCode() {
        return Objects.hash(problemDetails);
    }

    @Override
    public String toString() {
        return "ValidationException{" +
                "problemDetails=" + problemDetails +
                '}';
    }
}
