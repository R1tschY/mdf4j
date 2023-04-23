/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.util.Optional;

@EqualsAndHashCode
public final class Link<T> {
    private static final Link<?> NIL = new Link<>(0);

    private final long link;
    private volatile T loaded;

    public Link(long link) {
        this.link = link;
    }

    @SuppressWarnings("unchecked")
    public static <T> Link<T> nil() {
        return (Link<T>) NIL;
    }

    public static <T> Link<T> of(long link) {
        return link == 0 ? nil() : new Link<>(link);
    }

    public long asLong() {
        return link;
    }

    public boolean isNil() {
        return link == 0;
    }

    public <P extends FromBytesInput<T>> Optional<T> resolve(P resolver, ByteInput input) throws IOException {
        if (link != 0) {
            var loadedLocal = loaded;
            if (loadedLocal == null) {
                synchronized (this) {
                    loadedLocal = loaded;
                    if (loadedLocal == null) {
                        input.seek(link);
                        loadedLocal = loaded = resolver.parse(input);
                    }
                }
            }

            return Optional.of(loadedLocal);
        } else {
            return Optional.empty();
        }
    }

    public <P extends FromBytesInput<T>> Optional<T> resolveNonCached(P resolver, ByteInput input) throws IOException {
        if (link != 0) {
            input.seek(link);
            return Optional.of(resolver.parse(input));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return "Link{" + link + '}';
    }
}
