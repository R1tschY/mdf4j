/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import de.richardliebscher.mdf4.exceptions.InvalidTypeException;
import java.io.IOException;

/**
 * Visit value.
 *
 * @param <T> Deserialized value
 */
public interface Visitor<T> extends Expected {

  /**
   * Visit unsigned 8-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  default T visitU8(byte value) throws IOException {
    return visitU64(UnsignedByte.toLong(value));
  }

  /**
   * Visit unsigned 16-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  default T visitU16(short value) throws IOException {
    return visitU64(UnsignedShort.toLong(value));
  }

  /**
   * Visit unsigned 32-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  default T visitU32(int value) throws IOException {
    return visitU64(UnsignedInteger.toLong(value));
  }

  /**
   * Visit unsigned 64-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  default T visitU64(long value) throws IOException {
    throw new InvalidTypeException("unsigned integer " + value, this);
  }

  /**
   * Visit signed 8-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  default T visitI8(byte value) throws IOException {
    return visitI64(value);
  }

  /**
   * Visit signed 16-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  default T visitI16(short value) throws IOException {
    return visitI64(value);
  }

  /**
   * Visit signed 32-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  default T visitI32(int value) throws IOException {
    return visitI64(value);
  }

  /**
   * Visit signed 64-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  default T visitI64(long value) throws IOException {
    throw new InvalidTypeException("signed integer " + value, this);
  }

  /**
   * Visit 16-bit floating point.
   *
   * @param value Value
   * @return Deserialized value
   * @see Half
   */
  default T visitF16(short value) throws IOException {
    return visitF64(Half.toDouble(value));
  }

  /**
   * Visit 32-bit floating point.
   *
   * @param value Value
   * @return Deserialized value
   */
  default T visitF32(float value) throws IOException {
    return visitF64(value);
  }

  /**
   * Visit 64-bit floating point.
   *
   * @param value Value
   * @return Deserialized value
   */
  default T visitF64(double value) throws IOException {
    throw new InvalidTypeException("floating-point value " + value, this);
  }

  /**
   * Visit invalid value.
   *
   * @return Deserialized value
   */
  default T visitInvalid() throws IOException {
    throw new InvalidTypeException("invalid value", this);
  }
}
