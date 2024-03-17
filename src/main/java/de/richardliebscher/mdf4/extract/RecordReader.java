/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.Result;
import de.richardliebscher.mdf4.Result.Err;
import de.richardliebscher.mdf4.Result.Ok;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A record reader.
 *
 * @param <R> Deserialized record type
 */
public interface RecordReader<B, R> extends AutoCloseable {

  /**
   * Create an iterator for deserializing all elements the same way.
   *
   * @return Deserialized value
   */
  default Iterator<Result<R, IOException>> iterator() {
    return new Iterator<>() {
      private boolean error = false;
      private IOException exception;

      @Override
      public boolean hasNext() {
        try {
          return !error && RecordReader.this.hasNext();
        } catch (IOException exception) {
          this.exception = exception;
          return true;
        }
      }

      @Override
      public Result<R, IOException> next() {
        if (exception != null) {
          final var exception = this.exception;
          this.exception = null;
          this.error = true;
          return new Err<>(exception);
        }

        if (error) {
          throw new NoSuchElementException();
        }

        try {
          return new Ok<>(RecordReader.this.next());
        } catch (IOException exp) {
          error = true;
          return new Err<>(exp);
        }
      }
    };
  }

  /**
   * Return whether remaining records exist.
   *
   * @return {@code true} iff remaining records exist
   */
  boolean hasNext() throws IOException;

  /**
   * Read next record.
   *
   * @return Deserialized record
   * @throws IOException            Unable to read record from file
   * @throws NoSuchElementException No remaining records
   * @see #hasNext
   * @see #nextInto
   */
  R next() throws IOException, NoSuchElementException;

  /**
   * Read next record into place.
   *
   * @throws IOException            Unable to read record from file
   * @throws NoSuchElementException No remaining records
   * @see #hasNext
   * @see #next
   */
  void nextInto(B destination) throws IOException, NoSuchElementException;

  /**
   * Iterate over every remaining record.
   *
   * @param consumer Consumer for records
   * @throws IOException Unable to a read record from file
   */
  default void forEachRemaining(RecordConsumer<R> consumer) throws IOException {
    while (hasNext()) {
      consumer.consume(next());
    }
  }
}
