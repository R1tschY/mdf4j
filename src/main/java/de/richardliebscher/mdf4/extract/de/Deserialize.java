/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

/**
 * Interface to deserialize an arbitrary type.
 *
 * @param <T> Target type
 */
public interface Deserialize<T> {

  /**
   * Deserialize an arbitrary type.
   *
   * @param deserializer Format specific deserializer
   * @return Target
   * @throws IOException Unable to deserialize
   */
  T deserialize(Deserializer deserializer) throws IOException;
}
