/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Boxed object for unsigned integer.
 */
@RequiredArgsConstructor
@Getter
public class UnsignedInteger extends Number {

  private final int value;

  public static long toLong(int value) {
    return Integer.toUnsignedLong(value);
  }

  @Override
  public int intValue() {
    return (int) longValue();
  }

  @Override
  public long longValue() {
    return Integer.toUnsignedLong(value);
  }

  @Override
  public float floatValue() {
    return longValue();
  }

  @Override
  public double doubleValue() {
    return longValue();
  }

  @Override
  public String toString() {
    return String.valueOf(longValue());
  }

  /**
   * Return string representation.
   *
   * @param value Unsigned value
   * @return String representation
   */
  public static String toString(int value) {
    return Long.toString(Integer.toUnsignedLong(value));
  }

  /**
   * Return string representation.
   *
   * @param value Unsigned value
   * @param radix Radix
   * @return String representation
   */
  public static String toString(int value, int radix) {
    return Long.toString(Integer.toUnsignedLong(value), radix);
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof UnsignedInteger && this.value == ((UnsignedInteger) o).value;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(this.value);
  }
}
