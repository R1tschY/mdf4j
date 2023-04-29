/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Iterators {

  public static <T> Stream<T> streamBlockSeq(Iterator<T> iterator) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator,
        Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
  }

  public static <T> Iterable<T> asIterable(Iterable<T> iterable) {
    return iterable;
  }

  public static <T, E extends Throwable> void forEach(Iterator<T> iterator,
      TryConsume<T, E> consume) throws E {
    while (iterator.hasNext()) {
      consume.consume(iterator.next());
    }
  }

  public static <T, E extends Throwable> Optional<T> find(Iterator<T> iterator,
      TryPredicate<T, E> predicate) throws E {
    while (iterator.hasNext()) {
      final var value = iterator.next();
      if (predicate.test(value)) {
        return Optional.of(value);
      }
    }
    return Optional.empty();
  }

  public interface TryConsume<T, E extends Throwable> {

    void consume(T value) throws E;
  }

  public interface TryPredicate<T, E extends Throwable> {

    boolean test(T value) throws E;
  }
}
