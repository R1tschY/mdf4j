/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.math.BigInteger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Boxed object for unsigned long.
 */
@RequiredArgsConstructor
@Getter
public class UnsignedLong extends Number {

  private static final long LONG_UNSIGNED_MASK = 0x7fffffffffffffffL;

  private final long value;

  @Override
  public int intValue() {
    return (int) longValue();
  }

  @Override
  public long longValue() {
    return value & LONG_UNSIGNED_MASK;
  }

  @Override
  public float floatValue() {
    return toFloatValue(value);
  }

  /**
   * Return value as {@code float}.
   *
   * @param value Unsigned value
   * @return Value as {@code float}
   */
  public static float toFloatValue(long value) {
    return value < 0 ? ((value >>> 1) | (value & 1)) * 2.0f : value;
  }

  @Override
  public double doubleValue() {
    return toDoubleValue(value);
  }

  /**
   * Return value as {@code double}.
   *
   * @param value Unsigned value
   * @return Value as {@code double}
   */
  public static double toDoubleValue(long value) {
    return value < 0 ? ((value >>> 1) | (value & 1)) * 2.0 : value;
  }

  /**
   * Return value as {@link BigInteger}.
   *
   * @param value Unsigned value
   * @return Value as {@link BigInteger}
   */
  public static BigInteger toBigIntegerValue(long value) {
    var bigIntegerValue = BigInteger.valueOf(value & LONG_UNSIGNED_MASK);
    if (value < 0) {
      return bigIntegerValue.setBit(Long.SIZE - 1);
    } else {
      return bigIntegerValue;
    }
  }

  /**
   * Return string representation.
   *
   * @param value Unsigned value
   * @return String representation
   */
  public static String toString(long value) {
    return Long.toUnsignedString(value);
  }

  /**
   * Return string representation.
   *
   * @param value Unsigned value
   * @param radix Radix
   * @return String representation
   */
  public static String toString(long value, int radix) {
    return Long.toUnsignedString(value, radix);
  }

  @Override
  public String toString() {
    return Long.toUnsignedString(value);
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof UnsignedLong && this.value == ((UnsignedLong) o).value;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.value);
  }
}
