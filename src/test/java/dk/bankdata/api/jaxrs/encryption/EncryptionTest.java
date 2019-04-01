package dk.bankdata.api.jaxrs.encryption;

import org.junit.Assert;
import org.junit.Test;

public class EncryptionTest {
    @Test
    public void shouldEncryptStringWithOptionNone() {
        String seed = "Secret-Text";
        Encryption encryption = new Encryption("ThisIsOneLongCipherKeyWhichIsOdd");

        String encrypted = encryption.encrypt(seed);

        Assert.assertEquals("Sxzgi2b+T92FTmr5UNw2nA==", encrypted);
    }

    @Test
    public void shouldEncryptString() {
        String seed = "Secret-Text";
        Encryption encryption = new Encryption("ThisIsOneLongCipherKeyWhichIsOdd");

        String encrypted = encryption.encrypt(seed, EncryptionOption.NONE);

        Assert.assertEquals("Sxzgi2b+T92FTmr5UNw2nA==", encrypted);
    }

    @Test
    public void shouldEncryptStringAndUseUrlEncode() {
        String seed = "Secret-Text";
        Encryption encryption = new Encryption("ThisIsOneLongCipherKeyWhichIsOdd");

        String encrypted = encryption.encrypt(seed, EncryptionOption.URL_ENCODE);

        Assert.assertEquals("Sxzgi2b-T92FTmr5UNw2nA==", encrypted);
    }

    @Test
    public void shouldDecryptString() {
        String seed = "Sxzgi2b+T92FTmr5UNw2nA==";
        Encryption encryption = new Encryption("ThisIsOneLongCipherKeyWhichIsOdd");

        String decrypted = encryption.decrypt(seed);

        Assert.assertEquals("Secret-Text", decrypted);
    }
}