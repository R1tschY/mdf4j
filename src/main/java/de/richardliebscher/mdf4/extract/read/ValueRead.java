package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.io.ByteInput;

import java.io.IOException;

public interface ValueRead {
    <T> T read(ByteInput input, Visitor<T> visitor) throws IOException;
}
