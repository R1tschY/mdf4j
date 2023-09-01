/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

/**
 * Deserialize into a structure.
 *
 * @param <T> Structure type
 */
public interface DeserializeInto<T> {

  /**
   * Deserialize into a structure.
   *
   * @param deserializer Deserializer
   * @param dest         Structure
   * @throws IOException Unable to deserialize
   */
  void deserializeInto(Deserializer deserializer, T dest) throws IOException;
}
