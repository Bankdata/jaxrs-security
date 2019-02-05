package dk.bankdata.api.jaxrs.jwt;

import static org.mockito.Mockito.mock;

import dk.bankdata.api.types.ProblemDetails;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValidationExceptionTest {
    @Test
    public void shouldCreateExceptionWithProblemDetails() {
        ProblemDetails problemDetails = mock(ProblemDetails.class);
        Exception exception = mock(Exception.class);

        ValidationException result = new ValidationException(problemDetails, exception);

        Assert.assertSame(result.getProblemDetails(), problemDetails);
        Assert.assertSame(result.getCause(), exception);
    }

}