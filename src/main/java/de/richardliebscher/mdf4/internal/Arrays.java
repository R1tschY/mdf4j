package de.richardliebscher.mdf4.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Array;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Arrays {
    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(Class<T> clazz, int length) {
        return (T[]) Array.newInstance(clazz, length);
    }
}
