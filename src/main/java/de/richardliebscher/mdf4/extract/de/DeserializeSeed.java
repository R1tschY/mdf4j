/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

/**
 * Interface to deserialize an arbitrary type using external seed.
 *
 * @param <T> Target type
 */
public interface DeserializeSeed<T> {

  /**
   * Get {@link DeserializeSeed} using empty seed.
   *
   * @param <T> Target type
   * @return {@link DeserializeSeed} with empty seed
   */
  static <T> DeserializeSeed<T> empty() {
    return Deserialize::deserialize;
  }

  /**
   * Deserialize an arbitrary type using external seed.
   *
   * @param deserialize Real {@link Deserialize} interface
   * @param deserializer Format specific deserializer
   * @return Target
   * @throws IOException Unable to deserialize
   */
  T deserialize(Deserialize<T> deserialize, Deserializer deserializer) throws IOException;
}
