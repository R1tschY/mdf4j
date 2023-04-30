/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import java.io.IOException;

/**
 * Read structure from file.
 *
 * @param <T> Target type
 */
public interface FromBytesInput<T> {

  /**
   * Parse from input file.
   *
   * @param input Input file
   * @return Value
   * @throws IOException Unable to read structure from file
   */
  T parse(ByteInput input) throws IOException;
}
