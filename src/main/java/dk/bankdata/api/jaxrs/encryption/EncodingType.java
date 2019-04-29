package dk.bankdata.api.jaxrs.encryption;

import java.util.Base64;
import java.util.function.Function;

public enum EncodingType {
    BASE_64(s -> Base64.getEncoder().encodeToString(s)),
    URL_ENCODE(s -> Base64.getUrlEncoder().encodeToString(s));

    private final Function<byte[],String> encoder;

    EncodingType(Function<byte[], String> encoder) {
        this.encoder = encoder;
    }

    public String encode(byte[] data) {
        return encoder.apply(data);
    }
}

