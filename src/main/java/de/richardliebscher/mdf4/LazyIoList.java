/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.Result.Err;
import de.richardliebscher.mdf4.Result.Ok;
import java.io.IOException;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Iterate over items in MDF file lazily, so I/O errors are possible.
 *
 * <p>For Java standard library interfaces the {@link Result} type is used to wrap exceptions.
 *
 * @param <T> Item type
 */
public interface LazyIoList<T> extends Iterable<Result<T, IOException>> {

  /**
   * Fast iterator that throws wile iterating.
   *
   * <pre>{@code
   *   final var iter = list.iter();
   *   T item;
   *   while ((item = iter.next()) != null) {
   *     System.out.println(item);
   *   }
   * }</pre>
   *
   * @return {@link LazyIoIterator}
   */
  LazyIoIterator<T> iter();

  @Override
  default void forEach(Consumer<? super Result<T, IOException>> action) {
    final var iter = iter();
    T elem;
    try {
      while ((elem = iter.next()) != null) {
        action.accept(new Ok<>(elem));
      }
    } catch (IOException e) {
      action.accept(new Err<>(e));
    }
  }

  @Override
  default Iterator<Result<T, IOException>> iterator() {
    final var iter = iter();
    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      public Result<T, IOException> next() {
        try {
          return new Ok<>(iter.next());
        } catch (IOException e) {
          return new Err<>(e);
        }
      }

      @Override
      public void forEachRemaining(Consumer<? super Result<T, IOException>> action) {
        T elem;
        try {
          while ((elem = iter.next()) != null) {
            action.accept(new Ok<>(elem));
          }
        } catch (IOException e) {
          action.accept(new Err<>(e));
        }
      }
    };
  }

  /**
   * Stream items.
   *
   * <pre>{@code
   *   list.stream().forEachOrdered(res -> {
   *     try {
   *       System.out.println(res.get()));
   *     } catch (IOException exp) {
   *       System.err.println(exp);
   *     }
   *   });
   * }</pre>
   *
   * @return Stream of result items
   */
  default Stream<Result<T, IOException>> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  @Override
  default Spliterator<Result<T, IOException>> spliterator() {
    final var iter = iter();
    return new Spliterator<>() {
      @Override
      public boolean tryAdvance(Consumer<? super Result<T, IOException>> action) {
        try {
          final var elem = iter.next();
          if (elem == null) {
            return false;
          }
          action.accept(new Ok<>(elem));
        } catch (IOException e) {
          action.accept(new Err<>(e));
        }
        return true;
      }

      @Override
      public void forEachRemaining(Consumer<? super Result<T, IOException>> action) {
        T elem;
        try {
          while ((elem = iter.next()) != null) {
            action.accept(new Ok<>(elem));
          }
        } catch (IOException e) {
          action.accept(new Err<>(e));
        }
      }

      @Override
      public Spliterator<Result<T, IOException>> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        return Long.MAX_VALUE;
      }

      @Override
      public int characteristics() {
        return Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE;
      }
    };
  }
}
