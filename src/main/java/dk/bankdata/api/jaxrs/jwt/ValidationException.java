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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationException that = (ValidationException) o;
        return Objects.equals(problemDetails, that.problemDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(problemDetails);
    }

    @Override
    public String toString() {
        return "AdvisorException{" +
                "problemDetails=" + problemDetails +
                '}';
    }
}
