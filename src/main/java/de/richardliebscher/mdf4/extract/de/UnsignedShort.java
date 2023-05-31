/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Boxed object for unsigned short.
 */
@RequiredArgsConstructor
@Getter
public final class UnsignedShort extends Number {

  private final short value;

  public static int toInt(short value) {
    return Short.toUnsignedInt(value);
  }

  public static long toLong(short value) {
    return Short.toUnsignedLong(value);
  }

  @Override
  public int intValue() {
    return Short.toUnsignedInt(value);
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
  public static String toString(short value) {
    return Integer.toString(Short.toUnsignedInt(value));
  }

  /**
   * Return string representation.
   *
   * @param value Unsigned value
   * @param radix Radix
   * @return String representation
   */
  public static String toString(short value, int radix) {
    return Integer.toString(Short.toUnsignedInt(value), radix);
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof UnsignedShort && this.value == ((UnsignedShort) o).value;
  }

  @Override
  public int hashCode() {
    return Short.hashCode(this.value);
  }
}
