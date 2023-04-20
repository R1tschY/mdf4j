package de.richardliebscher.mdf4.io;

import de.richardliebscher.mdf4.exceptions.ParseException;

import java.io.IOException;
import java.util.Optional;

public interface FromBytesInput<T> {
    T parse(ByteInput input) throws IOException;

    default Optional<T> maybeParseAt(long pos, ByteInput input) throws IOException {
        if (pos != 0) {
            input.seek(pos);
            return Optional.of(parse(input));
        } else {
            return Optional.empty();
        }
    }
}
