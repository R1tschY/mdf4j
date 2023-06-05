/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.datatypes;

/**
 * Signed integer type.
 */
public class IntegerType implements DataType {
  private final int bitCount;

  public IntegerType(int bitCount) {
    this.bitCount = bitCount;
  }

  public int getBitCount() {
    return bitCount;
  }

  @Override
  public <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E {
    return visitor.visit(this);
  }
}
