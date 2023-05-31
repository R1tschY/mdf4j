/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

/**
 * Half-precision floating-point following the IEEE 754-2008 standard (binary16).
 */
public class Half extends Number {

  /**
   * Positive infinity.
   */
  public static final short POSITIVE_INFINITY = 0x7C00;

  /**
   * Negative infinity.
   */
  public static final short NEGATIVE_INFINITY = (short) 0xFC00;

  /**
   * Not-a-Number (NaN) value.
   */
  public static final short NaN = 0x7C01;

  /**
   * Largest positive finite value.
   */
  public static final short MAX_VALUE = 0x7BFF;

  /**
   * Smallest positive normal value.
   */
  public static final short MIN_NORMAL = 0x0400;

  /**
   * Smallest positive nonzero value.
   */
  public static final short MIN_VALUE = 0x0001;

  /**
   * Maximum exponent for finite values.
   */
  public static final int MAX_EXPONENT = 15;

  /**
   * Minimum exponent a normalized values can have.
   */
  public static final int MIN_EXPONENT = -14;

  /**
   * The number of bits used to represent a value.
   */
  public static final int SIZE = 16;

  /**
   * The number of bytes used to represent a value.
   */
  public static final int BYTES = SIZE / Byte.SIZE;

  private final short value;

  private Half(short value) {
    this.value = value;
  }

  public static Half fromShortBits(short value) {
    return new Half(value);
  }

  /**
   * Returns {@code float} value corresponding to bit representation.
   *
   * @param bits Bits
   * @return {@code float} value for {@code bits}
   */
  public static float toFloat(short bits) {
    final var sign = bits & 0x8000;
    var significant = bits & 0x03ff;
    var exponent = bits & 0x7c00;

    if (exponent == 0) {
      if (significant != 0) {
        exponent = 0x1c400;
        do {
          significant <<= 1;
          exponent -= 0x400;
        } while ((significant & 0x400) == 0);
        significant &= 0x3ff;
      }
    } else if (exponent == 0x7c00) {
      exponent = 0x3fc00;
    } else {
      exponent += 0x1c000;
    }
    return Float.intBitsToFloat(sign << 16 | (exponent | significant) << 13);
  }

  public static double toDouble(short bits) {
    // TODO: improve performance
    return toFloat(bits);
  }

  /**
   * Returns bit representation.
   *
   * @return Bit representation
   */
  public short rawShortBits() {
    return value;
  }

  /**
   * Returns bit representation with normalized NaN representation.
   *
   * @return Bit representation
   */
  public short shortBits() {
    if (isNaN(value)) {
      return NaN;
    }

    return value;
  }

  /**
   * Return whether value is Not-A-Number.
   *
   * @param bits Bit representation
   * @return {@code true} iff value is Not-A-Number
   */
  public boolean isNaN(short bits) {
    return (bits & 0x7c00) == 0x7c00 && (bits & 0x03ff) != 0;
  }

  /**
   * Return whether value is infinite.
   *
   * @param bits Bit representation
   * @return {@code true} iff value is infinite
   */
  public static boolean isInfinite(short bits) {
    return (bits == POSITIVE_INFINITY) || (bits == NEGATIVE_INFINITY);
  }

  /**
   * Return whether value is finite.
   *
   * @param bits Bit representation
   * @return {@code true} iff value is finite
   */
  public static boolean isFinite(short bits) {
    return (bits & 0x7c00) != 0x7c00;
  }

  @Override
  public int intValue() {
    return (int) toFloat(value);
  }

  @Override
  public long longValue() {
    return (long) toFloat(value);
  }

  @Override
  public float floatValue() {
    return toFloat(value);
  }

  @Override
  public double doubleValue() {
    return toFloat(value);
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof Half) && (((Half) obj).value == value);
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public String toString() {
    return toString(value);
  }

  /**
   * Return string representation.
   *
   * @param value Half-precision floating-point value
   * @return String representation
   */
  public static String toString(short value) {
    return Float.toString(toFloat(value));
  }
}
