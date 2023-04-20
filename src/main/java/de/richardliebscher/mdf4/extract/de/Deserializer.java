package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

public interface Deserializer {
    <R> R deserialize_row(RecordVisitor<R> recordVisitor) throws IOException;
    <R> R deserialize_value(Visitor<R> visitor) throws IOException;
}
