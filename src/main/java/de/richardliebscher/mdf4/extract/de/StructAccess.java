/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

/**
 * Access to structure composition.
 */
public interface StructAccess {

  /**
   * Return number of fields.
   *
   * @return Number of fields
   */
  int fields();

  /**
   * Deserialize next field.
   *
   * @param deserialize {@link Deserialize} interface
   * @param <T>         Type to deserialize
   * @return Deserialized value
   */
  <T> T next_field(Deserialize<T> deserialize) throws IOException;
}
