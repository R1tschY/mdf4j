package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.io.ByteInput;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class NoValueRead implements ValueRead {
    private final Deserializer deserializer;

    @Override
    public <T> T read(ByteInput input, Visitor<T> visitor) throws IOException {
        return deserializer.deserialize_value(visitor);
    }
}
