package dk.bankdata.api.jaxrs.encryption;

import java.util.Base64;
import java.util.function.Function;

public enum DecodingType {

    BASE_64(s -> Base64.getDecoder().decode(s)),
    URL_ENCODE(s -> Base64.getUrlDecoder().decode(s));

    private final Function<byte[], byte[]> decoder;

    DecodingType(Function<byte[], byte[]> decoder) {
        this.decoder = decoder;
    }

    public byte[] decode(byte[] data) {
        return decoder.apply(data);
    }

}

