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

        String encrypted = encryption.encrypt(seed, EncodingType.BASE_64);

        Assert.assertEquals("Sxzgi2b+T92FTmr5UNw2nA==", encrypted);
    }

    @Test
    public void shouldEncryptStringAndReturnUnsafeUrl() {
        String seed = "9999-0009999999";
        Encryption encryption = new Encryption("ThisIsOneLongCipherKeyWhichIsOdd");

        String encrypted = encryption.encrypt(seed, EncodingType.BASE_64);

        Assert.assertEquals("pJes9E5oZ/JQ2LoMV4ZZiQ==", encrypted);
    }

    @Test
    public void shouldEncryptStringAndUseUrlEncode() {
        String seed = "Secret-Text";
        Encryption encryption = new Encryption("ThisIsOneLongCipherKeyWhichIsOdd");

        String encrypted = encryption.encrypt(seed, EncodingType.URL_ENCODE);

        Assert.assertEquals("Sxzgi2b-T92FTmr5UNw2nA==", encrypted);
    }

    @Test
    public void shouldDecryptStringAndUseUrlEncode() {
        String seed = "Sxzgi2b-T92FTmr5UNw2nA==";
        Encryption encryption = new Encryption("ThisIsOneLongCipherKeyWhichIsOdd");

        String decrypted = encryption.decrypt(seed, DecodingType.URL_ENCODE);

        Assert.assertEquals("Secret-Text", decrypted);

    }

    @Test
    public void shouldEncryptStringAndReturnSafeUrl() {
        String seed = "9999-0009999999";
        Encryption encryption = new Encryption("ThisIsOneLongCipherKeyWhichIsOdd");

        String encrypted = encryption.encrypt(seed, EncodingType.URL_ENCODE);

        Assert.assertEquals("pJes9E5oZ_JQ2LoMV4ZZiQ==", encrypted);
    }

    @Test
    public void shouldDecryptSafeUrlString() {
        String seed = "pJes9E5oZ_JQ2LoMV4ZZiQ==";
        Encryption encryption = new Encryption("ThisIsOneLongCipherKeyWhichIsOdd");

        String decrypted = encryption.decrypt(seed, DecodingType.URL_ENCODE);

        Assert.assertEquals("9999-0009999999", decrypted);

    }
}