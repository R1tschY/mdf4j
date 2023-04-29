/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

public interface Visitor<T> {

  T visitU8(byte value);

  T visitU16(short value);

  T visitU32(int value);

  T visitU64(long value);

  T visitI8(byte value);

  T visitI16(short value);

  T visitI32(int value);

  T visitI64(long value);

  T visitF32(float value);

  T visitF64(double value);

  T visitInvalid();
}
