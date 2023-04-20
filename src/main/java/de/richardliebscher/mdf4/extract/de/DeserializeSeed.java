package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

public interface DeserializeSeed<T> {
    static final DeserializeSeed<?> EMPTY = (DeserializeSeed<Object>) Deserialize::deserialize;

    @SuppressWarnings("unchecked")
    static <T> DeserializeSeed<T> empty() {
        return (DeserializeSeed<T>) EMPTY;
    }

    T deserialize(Deserialize<T> deserialize, Deserializer deserializer) throws IOException;
}
