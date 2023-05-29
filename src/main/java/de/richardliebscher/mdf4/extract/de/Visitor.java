/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

/**
 * Visit value.
 *
 * @param <T> Deserialized value
 */
public interface Visitor<T> {

  /**
   * Visit unsigned 8-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  T visitU8(byte value);

  /**
   * Visit unsigned 16-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  T visitU16(short value);

  /**
   * Visit unsigned 32-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  T visitU32(int value);

  /**
   * Visit unsigned 64-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  T visitU64(long value);

  /**
   * Visit signed 8-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  T visitI8(byte value);

  /**
   * Visit signed 16-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  T visitI16(short value);

  /**
   * Visit signed 32-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  T visitI32(int value);

  /**
   * Visit signed 64-bit integer.
   *
   * @param value Value
   * @return Deserialized value
   */
  T visitI64(long value);

  /**
   * Visit 16-bit floating point.
   *
   * @param value Value
   * @return Deserialized value
   * @see Half
   */
  T visitF16(short value);

  /**
   * Visit 32-bit floating point.
   *
   * @param value Value
   * @return Deserialized value
   */
  T visitF32(float value);

  /**
   * Visit 64-bit floating point.
   *
   * @param value Value
   * @return Deserialized value
   */
  T visitF64(double value);

  /**
   * Visit invalid value.
   *
   * @return Deserialized value
   */
  T visitInvalid();
}
