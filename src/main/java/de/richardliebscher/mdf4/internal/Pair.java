package de.richardliebscher.mdf4.internal;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Map;

@ToString
@EqualsAndHashCode
@Getter
@RequiredArgsConstructor(staticName = "of")
public class Pair<T1, T2> implements Map.Entry<T1, T2> {
    private final T1 left;
    private final T2 right;

    @Override
    public T1 getKey() {
        return left;
    }

    @Override
    public T2 getValue() {
        return right;
    }

    @Override
    public T2 setValue(T2 value) {
        throw new UnsupportedOperationException();
    }
}
