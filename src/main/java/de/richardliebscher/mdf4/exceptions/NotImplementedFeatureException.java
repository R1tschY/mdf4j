package de.richardliebscher.mdf4.exceptions;

public class NotImplementedFeatureException extends ParseException {

    public NotImplementedFeatureException(String message) {
        super(message);
    }

    public NotImplementedFeatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotImplementedFeatureException(Throwable cause) {
        super(cause);
    }
}
