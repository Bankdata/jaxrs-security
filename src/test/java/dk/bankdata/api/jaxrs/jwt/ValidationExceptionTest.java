package dk.bankdata.api.jaxrs.jwt;

import static org.mockito.Mockito.mock;

import dk.bankdata.api.types.ErrorDetails;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValidationExceptionTest {
    @Test
    public void shouldCreateExceptionWithErrorDetails() {
        ErrorDetails errorDetails = mock(ErrorDetails.class);
        Exception exception = mock(Exception.class);

        ValidationException result = new ValidationException(errorDetails, exception);

        Assert.assertSame(result.getProblemDetails(), errorDetails);
        Assert.assertSame(result.getCause(), exception);
    }
}