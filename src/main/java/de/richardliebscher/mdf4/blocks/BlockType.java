/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;

public interface BlockType<T> {
  BlockTypeId id();

  /**
   * Parse from input file.
   *
   * @param input Input file
   * @return Value
   * @throws IOException Unable to read structure from file
   */
  T parse(ByteInput input) throws IOException;
}
