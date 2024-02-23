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
   * @param param   Parameter
   * @param <R>     Value type
   * @param <P>     Parameter type
   * @return Value
   * @throws IOException Unable to deserialize
   */
  <R, P> R deserialize_value(Visitor<R, P> visitor, P param) throws IOException;

  /**
   * Ignore value.
   *
   * @throws IOException Unable to skip value
   */
  void ignore() throws IOException;
}
