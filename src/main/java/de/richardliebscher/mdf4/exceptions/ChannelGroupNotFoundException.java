package de.richardliebscher.mdf4.exceptions;

public class ChannelGroupNotFoundException extends Exception {
    public ChannelGroupNotFoundException(String message) {
        super(message);
    }

    public ChannelGroupNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChannelGroupNotFoundException(Throwable cause) {
        super(cause);
    }
}
