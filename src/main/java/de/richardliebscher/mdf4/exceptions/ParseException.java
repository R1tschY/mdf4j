package de.richardliebscher.mdf4.exceptions;

import java.io.IOException;

public class ParseException extends IOException {
    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseException(Throwable cause) {
        super(cause);
    }
}
