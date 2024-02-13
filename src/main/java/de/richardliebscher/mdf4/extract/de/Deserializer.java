/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

/**
 * Format specific deserializer.
 *
 * <p>
 * Normally not implemented by a user.
 * </p>
 */
public interface Deserializer {

  /**
   * Deserialize a value using visitor.
   *
   * @param visitor Visitor
   * @param <R>     Value type
   * @return Value
   * @throws IOException Unable to deserialize
   */
  <R> R deserialize_value(Visitor<R> visitor) throws IOException;

  /**
   * Ignore value.
   *
   * @throws IOException Unable to skip value
   */
  void ignore() throws IOException;
}
