package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

public class InvalidDeserializer implements Deserializer {
    @Override
    public <R> R deserialize_row(RecordVisitor<R> recordVisitor) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> R deserialize_value(Visitor<R> visitor) throws IOException {
        return visitor.visitInvalid();
    }
}
