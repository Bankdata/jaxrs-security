package dk.bankdata.api.jaxrs.encryption;

import dk.bankdata.api.jaxrs.environment.Environment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EncryptionTest {
    @InjectMocks Encryption encryption;

    @Mock Environment environment;

    @Test
    public void shouldEncryptString() {
    }

    @Test
    public void shouldDecryptString() {
    }
}