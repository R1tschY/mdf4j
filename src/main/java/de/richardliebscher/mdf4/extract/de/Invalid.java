package de.richardliebscher.mdf4.extract.de;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Invalid {
    private static final int HASH = ThreadLocalRandom.current().nextInt();

    private static final Invalid INSTANCE = new Invalid();

    public static Invalid get() {
        return INSTANCE;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Invalid;
    }

    @Override
    public int hashCode() {
        return HASH;
    }

    @Override
    public String toString() {
        return "N/A";
    }
}
