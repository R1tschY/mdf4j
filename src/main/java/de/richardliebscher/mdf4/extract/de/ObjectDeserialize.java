/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

/**
 * Deserialize into an object.
 *
 * <p>
 * Unsigned values have own unsigned classes and invalid values are return {@link Invalid}
 * instances.
 * </p>
 *
 * @see Invalid
 * @see UnsignedByte
 * @see UnsignedShort
 * @see UnsignedInteger
 * @see UnsignedLong
 */
public class ObjectDeserialize implements Deserialize<Object> {

  private static final Visitor VISITOR = new Visitor();

  @Override
  public Object deserialize(Deserializer deserializer) throws IOException {
    return deserializer.deserialize_value(VISITOR, null);
  }

  private static class Visitor implements
      de.richardliebscher.mdf4.extract.de.Visitor<Object, Void> {

    @Override
    public String expecting() {
      return "any value";
    }

    @Override
    public Object visitU8(byte value, Void param) {
      return new UnsignedByte(value);
    }

    @Override
    public Object visitU16(short value, Void param) {
      return new UnsignedShort(value);
    }

    @Override
    public Object visitU32(int value, Void param) {
      return new UnsignedInteger(value);
    }

    @Override
    public Object visitU64(long value, Void param) {
      return new UnsignedLong(value);
    }

    @Override
    public Object visitI8(byte value, Void param) {
      return value;
    }

    @Override
    public Object visitI16(short value, Void param) {
      return value;
    }

    @Override
    public Object visitI32(int value, Void param) {
      return value;
    }

    @Override
    public Object visitI64(long value, Void param) {
      return value;
    }

    @Override
    public Object visitF16(short value, Void param) {
      return Half.fromShortBits(value);
    }

    @Override
    public Object visitF32(float value, Void param) {
      return value;
    }

    @Override
    public Object visitF64(double value, Void param) {
      return value;
    }

    @Override
    public Object visitInvalid(Void param) {
      return Invalid.get();
    }
  }
}
