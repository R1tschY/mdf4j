/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import de.richardliebscher.mdf4.Result;
import de.richardliebscher.mdf4.Result.Err;
import de.richardliebscher.mdf4.Result.Ok;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Access to elements in a record.
 */
public interface RecordAccess {

  /**
   * Deserialize next element using seed.
   *
   * @param deserialize {@link Deserialize} interface
   * @param seed        Seed
   * @param <S>         Seed type
   * @param <T>         Value type
   * @return Deserialized value
   * @throws IOException            Unable to deserialize
   * @throws NoSuchElementException No remaining element
   */
  <S extends DeserializeSeed<T>, T> T nextElementSeed(Deserialize<T> deserialize, S seed)
      throws IOException, NoSuchElementException;

  /**
   * Deserialize next element.
   *
   * @param deserialize {@link Deserialize} interface
   * @param <T>         Value type
   * @return Deserialized value
   * @throws IOException            Unable to deserialize
   * @throws NoSuchElementException No remaining element
   */
  default <T> T nextElement(Deserialize<T> deserialize) throws IOException, NoSuchElementException {
    return nextElementSeed(deserialize, DeserializeSeed.empty());
  }

  /**
   * Get remaining number of elements in record.
   *
   * @return number of elements in record
   */
  int remaining();

  /**
   * Create an iterator for deserializing all elements the same way.
   *
   * @param deserialize {@link Deserialize} interface
   * @param <T>         Value type
   * @return Deserialized value
   */
  default <T> Iterator<Result<T, IOException>> iterator(Deserialize<T> deserialize) {
    return new Iterator<>() {
      private long index = 0;
      private final long size = remaining();

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public Result<T, IOException> next() {
        try {
          index += 1;
          return new Ok<>(nextElement(deserialize));
        } catch (IOException e) {
          return new Err<>(e);
        }
      }
    };
  }
}
