package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

public interface Deserialize<T> {
    T deserialize(Deserializer deserializer) throws IOException;
}
