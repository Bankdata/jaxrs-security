package dk.bankdata.api.jaxrs.encryption;

import dk.bankdata.api.types.ProblemDetails;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class EncryptionExceptionTest {
    @Test
    public void shouldCreateExceptionWithProblemDetails() {
        ProblemDetails problemDetails = mock(ProblemDetails.class);
        Exception exception = mock(Exception.class);

        EncryptionException result = new EncryptionException(problemDetails, exception);

        Assert.assertSame(result.getProblemDetails(), problemDetails);
        Assert.assertSame(result.getCause(), exception);
    }
}