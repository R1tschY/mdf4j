/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Boxed object for unsigned byte.
 */
@RequiredArgsConstructor
@Getter
public final class UnsignedByte extends Number {

  private final byte value;

  public static int toInt(byte value) {
    return Byte.toUnsignedInt(value);
  }

  public static long toLong(byte value) {
    return Byte.toUnsignedLong(value);
  }

  @Override
  public int intValue() {
    return Byte.toUnsignedInt(value);
  }

  @Override
  public long longValue() {
    return intValue();
  }

  @Override
  public float floatValue() {
    return intValue();
  }

  @Override
  public double doubleValue() {
    return intValue();
  }

  @Override
  public String toString() {
    return toString(value);
  }

  /**
   * Return string representation.
   *
   * @param value Unsigned value
   * @return String representation
   */
  public static String toString(byte value) {
    return Integer.toString(Byte.toUnsignedInt(value));
  }

  /**
   * Return string representation.
   *
   * @param value Unsigned value
   * @param radix Radix
   * @return String representation
   */
  public static String toString(byte value, int radix) {
    return Integer.toString(Byte.toUnsignedInt(value), radix);
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof UnsignedByte && this.value == ((UnsignedByte) o).value;
  }

  @Override
  public int hashCode() {
    return Byte.hashCode(this.value);
  }
}
