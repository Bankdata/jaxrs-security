package dk.bankdata.api.jaxrs.encryption;

import static org.mockito.Mockito.when;

import dk.bankdata.api.jaxrs.environment.Environment;
import org.junit.Assert;
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
        when(environment.getCipherKey()).thenReturn("some-key-that-is-about-32-chars.");
        String encrypted = encryption.encrypt("some-text");

        Assert.assertTrue(encrypted.endsWith("=="));
    }

    @Test
    public void shouldDecryptString() {
        when(environment.getCipherKey()).thenReturn("some-key-that-is-about-32-chars.");
        String plainText = encryption.decrypt("CiUzRV9vLis9H7XAilZrig==");

        Assert.assertEquals("some-text", plainText);
    }
}