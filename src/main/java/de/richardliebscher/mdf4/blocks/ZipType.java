/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;

public enum ZipType {
  DEFLATE,
  TRANSPOSITION_DEFLATE;

  private static final ZipType[] VALUES = values();

  public static ZipType parse(int value) throws FormatException {
    if (value >= 0 && value < VALUES.length) {
      return VALUES[value];
    } else {
      throw new FormatException("Unknown zip type: " + value);
    }
  }
}
