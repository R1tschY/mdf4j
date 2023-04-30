/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

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
   * @throws IOException Unable to deserialize
   */
  <S extends DeserializeSeed<T>, T> T nextElementSeed(Deserialize<T> deserialize, S seed)
      throws IOException;

  /**
   * Deserialize next element.
   *
   * @param deserialize {@link Deserialize} interface
   * @param <T>         Value type
   * @return Deserialized value
   * @throws IOException Unable to deserialize
   */
  default <T> T nextElement(Deserialize<T> deserialize) throws IOException {
    return nextElementSeed(deserialize, DeserializeSeed.empty());
  }

  /**
   * Get number of elements in record.
   *
   * @return number of elements in record
   */
  int size();
}
